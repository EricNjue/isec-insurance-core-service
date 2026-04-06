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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

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

    @Transactional
    public void processEmail(MimeMessage message) {
        String messageId = null;
        String sender;
        String subject;
        try {
            messageId = message.getMessageID();
            sender = message.getFrom()[0].toString();
            subject = message.getSubject();

            if (auditRepository.existsByEmailMessageId(messageId)) {
                log.info("Email {} already processed, skipping", messageId);
                return;
            }

            CertificateIngestionAudit audit = auditRepository.save(CertificateIngestionAudit.builder()
                    .emailMessageId(messageId)
                    .sender(sender)
                    .subject(subject)
                    .status(IngestionStatus.RECEIVED)
                    .build());

            processInternal(message, audit);

        } catch (Exception e) {
            log.error("Failed to process email", e);
            if (messageId != null) {
                updateAuditStatus(messageId, IngestionStatus.FAILED, e.getMessage());
            }
        }
    }

    private void processInternal(MimeMessage message, CertificateIngestionAudit audit) throws MessagingException, IOException {
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

        if (parser == null) {
            log.warn("No parser found for email from {}", audit.getSender());
            audit.setStatus(IngestionStatus.FAILED);
            audit.setFailureReason("No suitable parser found for sender");
            auditRepository.save(audit);
            return;
        }

        ExtractedCertificateMetadata metadata = parser.parse(message);
        audit.setStatus(IngestionStatus.PARSED);
        auditRepository.save(audit);

        byte[] attachment = parser.extractAttachment(message);
        if (attachment == null) {
            log.warn("No PDF attachment found in email {}", audit.getEmailMessageId());
            audit.setStatus(IngestionStatus.FAILED);
            audit.setFailureReason("No PDF attachment found");
            auditRepository.save(audit);
            return;
        }

        // Matching Logic
        Optional<Policy> policyOpt = policyRepository.findByPolicyNumber(metadata.getPolicyNumber());
        if (policyOpt.isEmpty()) {
            log.warn("Policy {} not found for ingestion {}", metadata.getPolicyNumber(), audit.getEmailMessageId());
            audit.setStatus(IngestionStatus.MANUAL_REVIEW_REQUIRED);
            audit.setFailureReason("Policy not found: " + metadata.getPolicyNumber());
            auditRepository.save(audit);
            return;
        }

        Policy policy = policyOpt.get();
        
        // Check if certificate already exists (idempotency by policy and cert number)
        Optional<Certificate> existingCert = certificateRepository.findByPartnerCodeAndCertificateNumber(
                parser.getPartnerCode(), metadata.getCertificateNumber());
        
        if (existingCert.isPresent()) {
            log.info("Certificate {} already exists, ignoring duplicate", metadata.getCertificateNumber());
            audit.setStatus(IngestionStatus.DUPLICATE_IGNORED);
            auditRepository.save(audit);
            return;
        }

        // S3 Storage
        String year = String.valueOf(LocalDateTime.now().getYear());
        String month = String.format("%02d", LocalDateTime.now().getMonthValue());
        String s3Key = String.format("certificates/%s/%s/%s/%s/%s.pdf", 
                parser.getPartnerCode(), year, month, policy.getPolicyNumber(), metadata.getCertificateNumber());
        
        String checksum = DigestUtils.md5DigestAsHex(attachment);
        
        s3Service.uploadBytes(defaultBucket, s3Key, attachment, "application/pdf");
        
        audit.setStatus(IngestionStatus.STORED);
        auditRepository.save(audit);

        // Update/Create Certificate Record
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
                .emailReceivedAt(message.getReceivedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .status(CertificateStatus.ISSUED)
                .issuedAt(LocalDateTime.now())
                .startDate(metadata.getStartDate())
                .expiryDate(metadata.getExpiryDate())
                .build();

        certificate = certificateRepository.save(certificate);
        
        audit.setCertificateId(certificate.getId());
        audit.setStatus(IngestionStatus.COMPLETED);
        audit.setProcessedAt(LocalDateTime.now());
        auditRepository.save(audit);
        
        log.info("Successfully ingested certificate {} for policy {}", metadata.getCertificateNumber(), policy.getPolicyNumber());
    }

    private void updateAuditStatus(String messageId, IngestionStatus status, String reason) {
        auditRepository.findByEmailMessageId(messageId).ifPresent(audit -> {
            audit.setStatus(status);
            audit.setFailureReason(reason);
            auditRepository.save(audit);
        });
    }
}
