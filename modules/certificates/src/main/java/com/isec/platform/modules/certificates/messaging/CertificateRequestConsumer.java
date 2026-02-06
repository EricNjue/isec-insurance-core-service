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
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void handleCertificateRequest(CertificateRequestedEvent event) {
        log.info("Received certificate request event: {} for policy: {}", event.getEventId(), event.getPolicyNumber());
        
        if (idempotencyService.isDuplicate(event.getEventId())) {
            log.info("Duplicate event detected, skipping: {}", event.getEventId());
            return;
        }

        Certificate certificate = certificateRepository.findByIdempotencyKey(event.getEventId())
                .orElseGet(() -> {
                    log.warn("Certificate record not found for idempotency key: {}, creating new one", event.getEventId());
                    return Certificate.builder()
                            .policyId(event.getPolicyId())
                            .type(CertificateType.valueOf(event.getCertificateType()))
                            .startDate(event.getStartDate())
                            .expiryDate(event.getExpiryDate())
                            .idempotencyKey(event.getEventId())
                            .build();
                });

        if (certificate.getStatus() == CertificateStatus.ISSUED) {
            log.info("Certificate already issued for event: {}", event.getEventId());
            return;
        }

        try {
            certificate.setStatus(CertificateStatus.PROCESSING);
            certificateRepository.save(certificate);

            // 1. Call DMVIC API
            String dmvicRef = dmvicClient.issueCertificate(event.getRegistrationNumber(), event.getPolicyNumber());
            log.info("DMVIC issued certificate with reference: {}", dmvicRef);

            // 2. Update certificate record
            certificate.setDmvicReference(dmvicRef);
            certificate.setStatus(CertificateStatus.ISSUED);
            certificate.setIssuedAt(LocalDateTime.now());
            certificateRepository.save(certificate);
            
            log.info("Successfully issued {} certificate for policy: {}", event.getCertificateType(), event.getPolicyNumber());
            
            // 3. Trigger success notification event
            sendNotification(event, dmvicRef, "Insurance Certificate Issued", 
                    "Your " + event.getCertificateType() + " certificate for vehicle " + event.getRegistrationNumber() + " has been issued. Reference: " + dmvicRef);
            
        } catch (Exception e) {
            log.error("Failed to process certificate request for event: {}. Reason: {}", event.getEventId(), e.getMessage());
            certificate.setStatus(CertificateStatus.FAILED);
            certificateRepository.save(certificate);
            
            sendNotification(event, null, "Insurance Certificate Issuance Failed", 
                    "Failed to issue your " + event.getCertificateType() + " certificate for vehicle " + event.getRegistrationNumber() + ". Our team is looking into it.");
            
            throw e;
        }
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
