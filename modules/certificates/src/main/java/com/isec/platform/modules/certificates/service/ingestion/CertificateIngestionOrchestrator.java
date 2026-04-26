package com.isec.platform.modules.certificates.service.ingestion;

import com.isec.platform.modules.certificates.domain.Certificate;
import com.isec.platform.modules.certificates.domain.CertificateIngestionAudit;
import com.isec.platform.modules.certificates.domain.CertificateStatus;
import com.isec.platform.modules.certificates.domain.IngestionStatus;
import com.isec.platform.modules.certificates.dto.ExtractedCertificateMetadata;
import com.isec.platform.modules.certificates.repository.CertificateIngestionAuditRepository;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import com.isec.platform.modules.documents.service.S3Service;
import com.isec.platform.modules.policies.domain.Policy;
import com.isec.platform.modules.policies.repository.PolicyRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateIngestionOrchestrator {

    private final List<EmailParser> parsers;
    private final CertificateRepository certificateRepository;
    private final PolicyRepository policyRepository;
    private final CertificateIngestionAuditRepository auditRepository;
    private final S3Service s3Service;

    @Value("${ingestion.email.s3-bucket:isecdocuments}")
    private String defaultBucket;

    public boolean isCandidate(MimeMessage message) {
        return parsers.stream().anyMatch(p -> {
            try {
                return p.isCandidate(message);
            } catch (MessagingException e) {
                return false;
            }
        });
    }

    public Mono<Void> enqueueProcessing(MimeMessage message) {
        return Mono.fromCallable(() -> {
            String messageId = message.getMessageID();
            String sender = message.getFrom()[0].toString();
            String subject = message.getSubject();
            return new EmailInfo(messageId, sender, subject);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(info -> auditRepository.existsByEmailMessageId(info.messageId())
                .flatMap(exists -> {
                    if (exists) {
                        log.info("Email {} already processed or enqueued, skipping", info.messageId());
                        return Mono.empty();
                    }

                    return auditRepository.save(CertificateIngestionAudit.builder()
                            .emailMessageId(info.messageId())
                            .sender(info.sender())
                            .subject(info.subject())
                            .status(IngestionStatus.RECEIVED)
                            .createdAt(LocalDateTime.now())
                            .build())
                            .doOnSuccess(saved -> processEmailAsync(message, info.messageId()));
                }))
        .onErrorResume(e -> {
            log.error("Failed to enqueue email for processing", e);
            return Mono.empty();
        })
        .then();
    }

    @Async
    public void processEmailAsync(MimeMessage message, String messageId) {
        log.info("Starting async processing for email {}", messageId);
        
        auditRepository.updateStatusAtomic(messageId, IngestionStatus.RECEIVED.name(), IngestionStatus.PROCESSING.name())
            .flatMap(updated -> {
                if (updated == 0) {
                    log.info("Email {} already claimed or processed by another instance", messageId);
                    return Mono.empty();
                }

                return auditRepository.findByEmailMessageId(messageId)
                        .switchIfEmpty(Mono.error(new RuntimeException("Audit record not found for " + messageId)))
                        .flatMap(audit -> processInternal(message, audit));
            })
            .doOnError(e -> {
                log.error("Async processing failed for email {}", messageId, e);
                updateAuditStatus(messageId, IngestionStatus.FAILED, "Async processing error: " + e.getMessage()).subscribe();
            })
            .subscribe();
    }

    public void processEmail(MimeMessage message) {
        enqueueProcessing(message).subscribe();
    }

    private Mono<Void> processInternal(MimeMessage message, CertificateIngestionAudit audit) {
        return Mono.fromCallable(() -> {
            EmailParser parser = parsers.stream()
                    .filter(p -> {
                        try {
                            return p.canParse(message);
                        } catch (MessagingException e) {
                            return false;
                        }
                    })
                    .findFirst()
                    .orElse(null);

            return parser;
        })
        .flatMap(parser -> {
            if (parser == null) {
                log.warn("No parser found for email from {}", audit.getSender());
                audit.setStatus(IngestionStatus.FAILED);
                audit.setFailureReason("No suitable parser found for sender");
                return auditRepository.save(audit).then();
            }

            try {
                ExtractedCertificateMetadata metadata = parser.parse(message);
                audit.setStatus(IngestionStatus.PARSED);
                
                return auditRepository.save(audit)
                    .flatMap(savedAudit -> {
                        byte[] attachment;
                        try {
                            attachment = parser.extractAttachment(message);
                        } catch (Exception e) {
                            return Mono.error(e);
                        }
                        
                        if (attachment == null) {
                            log.warn("No PDF attachment found in email {}", audit.getEmailMessageId());
                            audit.setStatus(IngestionStatus.FAILED);
                            audit.setFailureReason("No PDF attachment found");
                            return auditRepository.save(audit).then();
                        }

                        return policyRepository.findByPolicyNumber(metadata.getPolicyNumber())
                            .switchIfEmpty(Mono.defer(() -> {
                                log.warn("Policy {} not found for ingestion {}", metadata.getPolicyNumber(), audit.getEmailMessageId());
                                audit.setStatus(IngestionStatus.MANUAL_REVIEW_REQUIRED);
                                audit.setFailureReason("Policy not found: " + metadata.getPolicyNumber());
                                return auditRepository.save(audit).then(Mono.empty());
                            }))
                            .flatMap(policy -> certificateRepository.findByPartnerCodeAndCertificateNumber(
                                    parser.getPartnerCode(), metadata.getCertificateNumber())
                                .flatMap(existingCert -> {
                                    log.info("Certificate {} already exists, ignoring duplicate", metadata.getCertificateNumber());
                                    audit.setStatus(IngestionStatus.DUPLICATE_IGNORED);
                                    return auditRepository.save(audit).then(Mono.empty());
                                })
                                .switchIfEmpty(Mono.defer(() -> {
                                    // S3 Storage
                                    String year = String.valueOf(LocalDateTime.now().getYear());
                                    String month = String.format("%02d", LocalDateTime.now().getMonthValue());
                                    String s3Key = String.format("certificates/%s/%s/%s/%s/%s.pdf", 
                                            parser.getPartnerCode(), year, month, policy.getPolicyNumber(), metadata.getCertificateNumber());
                                    
                                    String checksum = DigestUtils.md5DigestAsHex(attachment);
                                    
                                    audit.setStatus(IngestionStatus.STORED);
                                    return s3Service.uploadBytesAsync(defaultBucket, s3Key, attachment, "application/pdf")
                                        .then(auditRepository.save(audit))
                                        .flatMap(storedAudit -> {
                                            Certificate certificate = Certificate.builder()
                                                    .policyId(policy.getId())
                                                    .partnerCode(parser.getPartnerCode())
                                                    .certificateNumber(metadata.getCertificateNumber())
                                                    .policyNumber(metadata.getPolicyNumber())
                                                    .vehicleRegistrationNumber(metadata.getRegistrationNumber())
                                                    .chassisNumber(metadata.getChassisNumber())
                                                    .customerEmail(metadata.getCustomerEmail())
                                                    .s3Bucket(defaultBucket)
                                                    .s3Key(s3Key)
                                                    .fileName(metadata.getCertificateNumber() + ".pdf")
                                                    .fileSize((long) attachment.length)
                                                    .contentType("application/pdf")
                                                    .checksum(checksum)
                                                    .ingestionSource("EMAIL")
                                                    .emailMessageId(audit.getEmailMessageId())
                                                    .status(CertificateStatus.ISSUED)
                                                    .issuedAt(LocalDateTime.now())
                                                    .startDate(metadata.getStartDate())
                                                    .expiryDate(metadata.getExpiryDate())
                                                    .createdAt(LocalDateTime.now())
                                                    .updatedAt(LocalDateTime.now())
                                                    .build();

                                            return certificateRepository.save(certificate)
                                                .flatMap(savedCert -> {
                                                    audit.setCertificateId(savedCert.getId());
                                                    audit.setStatus(IngestionStatus.COMPLETED);
                                                    audit.setProcessedAt(LocalDateTime.now());
                                                    return auditRepository.save(audit).then();
                                                });
                                        });
                                })))
                                .then();
                    });
            } catch (Exception e) {
                return Mono.error(e);
            }
        });
    }

    public Mono<Void> recoverStuckItems() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        return auditRepository.findByStatusAndCreatedAtBefore(IngestionStatus.PROCESSING, threshold)
                .flatMap(item -> {
                    log.warn("Recovering stuck ingestion item: {}", item.getEmailMessageId());
                    item.setStatus(IngestionStatus.RECEIVED); // Allow retry
                    item.setFailureReason("Recovered from stuck PROCESSING state");
                    return auditRepository.save(item);
                }).then();
    }

    private Mono<Void> updateAuditStatus(String messageId, IngestionStatus status, String reason) {
        return auditRepository.findByEmailMessageId(messageId).flatMap(audit -> {
            audit.setStatus(status);
            audit.setFailureReason(reason);
            return auditRepository.save(audit);
        }).then();
    }

    private record EmailInfo(String messageId, String sender, String subject) {}
}
