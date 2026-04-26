package com.isec.platform.modules.documents.messaging;

import com.isec.platform.common.idempotency.service.IdempotencyService;
import com.isec.platform.messaging.RabbitMQConfig;
import com.isec.platform.messaging.events.NotificationChannel;
import com.isec.platform.messaging.events.NotificationSendEvent;
import com.isec.platform.messaging.events.ValuationLetterRequestedEvent;
import com.isec.platform.modules.documents.service.ValuationLetterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValuationLetterConsumer {

    private final RabbitTemplate rabbitTemplate;
    private final IdempotencyService idempotencyService;
    private final ValuationLetterService valuationLetterService;

    @RabbitListener(queues = "valuation.letter.requested.queue")
    public void handleValuationLetterRequest(ValuationLetterRequestedEvent event) {
        log.info("Received valuation letter request event: {} for policy: {}", event.getEventId(), event.getPolicyNumber());

        idempotencyService.isDuplicate(event.getEventId()); // Sync check for now

        valuationLetterService.generateIfNotExists(
                event.getPolicyId(),
                event.getInsuredName(),
                event.getRegistrationNumber(),
                true
        )
        .flatMap(letter -> {
            String downloadUrl = valuationLetterService.generateDownloadUrl(letter);
            NotificationSendEvent notificationEvent = NotificationSendEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .recipient(event.getRecipientEmail())
                    .channel(NotificationChannel.EMAIL)
                    .subject("Your Valuation Letter is Ready")
                    .content("Dear Customer, your valuation letter for vehicle " + event.getRegistrationNumber() + " is ready. Download: " + downloadUrl)
                    .correlationId(event.getCorrelationId())
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.NOTIFICATION_SEND_RK, notificationEvent);
            log.info("Notification event sent for valuation letter: {}", event.getPolicyNumber());
            return Mono.empty();
        })
        .doOnError(e -> log.error("Failed to process valuation letter request for event: {}. Reason: {}", event.getEventId(), e.getMessage()))
        .subscribe();
    }
}
