package com.isec.platform.modules.documents.messaging;

import com.isec.platform.common.idempotency.service.IdempotencyService;
import com.isec.platform.messaging.RabbitMQConfig;
import com.isec.platform.messaging.events.NotificationSendEvent;
import com.isec.platform.messaging.events.ValuationLetterRequestedEvent;
import com.isec.platform.modules.documents.domain.ApplicationDocument;
import com.isec.platform.modules.documents.repository.ApplicationDocumentRepository;
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
public class ValuationLetterConsumer {

    private final ApplicationDocumentRepository documentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final IdempotencyService idempotencyService;

    @RabbitListener(queues = "valuation.letter.requested.queue")
    @Transactional
    public void handleValuationLetterRequest(ValuationLetterRequestedEvent event) {
        log.info("Received valuation letter request event: {} for policy: {}", event.getEventId(), event.getPolicyNumber());

        if (idempotencyService.isDuplicate(event.getEventId())) {
            log.info("Duplicate event detected, skipping: {}", event.getEventId());
            return;
        }

        try {
            // 1. Mock Valuation Letter Generation & "Storage"
            String s3Key = "valuation-letters/" + event.getPolicyNumber() + "/valuation-" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            log.info("Generating valuation letter for vehicle {} and storing at {}", event.getRegistrationNumber(), s3Key);
            
            // In a real system, we'd use a PDF library and upload to S3 here.
            
            // 2. Persist document record
            ApplicationDocument document = ApplicationDocument.builder()
                    .applicationId(event.getPolicyId()) // Mapping policyId to applicationId for simplicity or we should find real appId
                    .documentType("VALUATION_LETTER")
                    .s3Key(s3Key)
                    .lastPresignedUrl("https://mock-s3-url.com/" + s3Key)
                    .urlExpiryAt(LocalDateTime.now().plusDays(7))
                    .createdAt(LocalDateTime.now())
                    .build();

            documentRepository.save(document);
            log.info("Successfully persisted valuation letter for policy: {}", event.getPolicyNumber());

            // 3. Trigger notification
            NotificationSendEvent notificationEvent = NotificationSendEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipient(event.getRecipientEmail())
                    .channel("EMAIL")
                    .subject("Your Valuation Letter is Ready")
                    .content("Dear Customer, your valuation letter for vehicle " + event.getRegistrationNumber() + " has been generated and is available for download.")
                    .correlationId(event.getCorrelationId())
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.NOTIFICATION_SEND_RK, notificationEvent);
            log.info("Notification event sent for valuation letter: {}", event.getPolicyNumber());

        } catch (Exception e) {
            log.error("Failed to process valuation letter request for event: {}. Reason: {}", event.getEventId(), e.getMessage());
            throw e;
        }
    }
}
