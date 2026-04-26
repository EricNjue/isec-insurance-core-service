package com.isec.platform.modules.certificates.messaging;

import com.isec.platform.common.idempotency.service.IdempotencyService;
import com.isec.platform.messaging.RabbitMQConfig;
import com.isec.platform.messaging.events.CertificateRequestedEvent;
import com.isec.platform.messaging.events.NotificationChannel;
import com.isec.platform.messaging.events.NotificationSendEvent;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.certificates.domain.Certificate;
import com.isec.platform.modules.certificates.domain.CertificateStatus;
import com.isec.platform.modules.certificates.domain.CertificateType;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import com.isec.platform.modules.integrations.dmvic.DmvicClient;
import com.isec.platform.modules.policies.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CertificateRequestConsumer {

    private final DmvicClient dmvicClient;
    private final CertificateRepository certificateRepository;
    private final ApplicationRepository applicationRepository;
    private final PolicyRepository policyRepository;
    private final RabbitTemplate rabbitTemplate;
    private final IdempotencyService idempotencyService;

    @RabbitListener(queues = RabbitMQConfig.CERTIFICATE_REQUESTED_QUEUE)
    public void handleCertificateRequest(CertificateRequestedEvent event) {
        log.info("Received certificate request event: {} for policy: {}", event.getEventId(), event.getPolicyNumber());
        
        idempotencyService.isDuplicate(event.getEventId())
                .flatMap(isDuplicate -> {
                    if (isDuplicate) {
                        log.info("Duplicate certificate request event: {}", event.getEventId());
                        return Mono.empty();
                    }
                    return certificateRepository.findByIdempotencyKey(event.getEventId())
                            .switchIfEmpty(Mono.defer(() -> {
                                log.warn("Certificate record not found for idempotency key: {}, creating new one", event.getEventId());
                                return certificateRepository.save(Certificate.builder()
                                        .policyId(event.getPolicyId())
                                        .type(CertificateType.valueOf(event.getCertificateType()))
                                        .status(CertificateStatus.PENDING)
                                        .startDate(event.getStartDate())
                                        .expiryDate(event.getExpiryDate())
                                        .idempotencyKey(event.getEventId())
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build());
                            }))
                            .flatMap(certificate -> {
                                if (certificate.getStatus() == CertificateStatus.ISSUED) {
                                    log.info("Certificate already issued for event: {}", event.getEventId());
                                    return Mono.empty();
                                }

                                certificate.setStatus(CertificateStatus.PROCESSING);
                                return certificateRepository.save(certificate)
                                        .flatMap(processingCert -> {
                                            // 1. Call DMVIC API - Assume dmvicClient.issueCertificate is reactive or we wrap it
                                            String dmvicRef = dmvicClient.issueCertificate(event.getRegistrationNumber(), event.getPolicyNumber());
                                            log.info("DMVIC issued certificate with reference: {}", dmvicRef);

                                            // 2. Update certificate record
                                            processingCert.setDmvicReference(dmvicRef);
                                            processingCert.setStatus(CertificateStatus.ISSUED);
                                            processingCert.setIssuedAt(LocalDateTime.now());
                                            processingCert.setUpdatedAt(LocalDateTime.now());

                                            return certificateRepository.save(processingCert)
                                                    .doOnNext(saved -> {
                                                        log.info("Successfully issued {} certificate for policy: {}", event.getCertificateType(), event.getPolicyNumber());
                                                        sendNotification(event, dmvicRef, "Insurance Certificate Issued",
                                                                "Your " + event.getCertificateType() + " certificate for vehicle " + event.getRegistrationNumber() + " has been issued. Reference: " + dmvicRef);
                                                    });
                                        });
                            });
                })
                .block(java.time.Duration.ofSeconds(10));
    }

    private void sendNotification(CertificateRequestedEvent event, String dmvicRef, String subject, String content) {
        if (event.getRecipientEmail() != null) {
            NotificationSendEvent emailEvent = NotificationSendEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipient(event.getRecipientEmail())
                    .channel(NotificationChannel.EMAIL)
                    .subject(subject)
                    .content(content)
                    .correlationId(event.getCorrelationId())
                    .build();
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.NOTIFICATION_SEND_RK, emailEvent);
            log.info("Notification event (EMAIL) sent for policy: {} to {}", event.getPolicyNumber(), event.getRecipientEmail());
        } else {
            log.warn("Recipient email not available in event for policy {}. Skipping EMAIL notification.", event.getPolicyNumber());
        }

        if (event.getRecipientPhoneNumber() != null) {
            NotificationSendEvent smsEvent = NotificationSendEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipient(event.getRecipientPhoneNumber())
                    .channel(NotificationChannel.SMS)
                    .content(content)
                    .correlationId(event.getCorrelationId())
                    .build();
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.NOTIFICATION_SEND_RK, smsEvent);
            log.info("Notification event (SMS) sent for policy: {} to {}", event.getPolicyNumber(), event.getRecipientPhoneNumber());
        } else {
            log.warn("Recipient phone number not available in event for policy {}. Skipping SMS notification.", event.getPolicyNumber());
        }
    }
}
