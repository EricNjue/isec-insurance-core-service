package com.isec.platform.modules.notifications.messaging;

import com.isec.platform.common.idempotency.service.IdempotencyService;
import com.isec.platform.messaging.RabbitMQConfig;
import com.isec.platform.messaging.events.NotificationChannel;
import com.isec.platform.messaging.events.NotificationSendEvent;
import com.isec.platform.modules.notifications.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final IdempotencyService idempotencyService;
    private final SmsService smsService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_SEND_QUEUE)
    public void handleNotificationSend(NotificationSendEvent event) {
        log.info("Received notification send event: {} for recipient: {}", event.getEventId(), event.getRecipient());

        idempotencyService.isDuplicate(event.getEventId())
                .flatMap(isDuplicate -> {
                    if (isDuplicate) {
                        return Mono.empty();
                    }

                    if (NotificationChannel.SMS.equals(event.getChannel())) {
                        return sendSms(event.getRecipient(), event.getContent());
                    } else {
                        return sendEmail(event.getRecipient(), event.getSubject(), event.getContent());
                    }
                })
                .subscribe(
                        v -> log.info("Successfully processed notification for event: {}", event.getEventId()),
                        e -> log.error("Failed to process notification for event: {}. Reason: {}", event.getEventId(), e.getMessage())
                );
    }

    private Mono<Void> sendSms(String recipient, String content) {
        log.info("Sending SMS to: {}, Content: {}", recipient, content);
        return smsService.sendSms(recipient, content);
    }

    private Mono<Void> sendEmail(String recipient, String subject, String content) {
        return Mono.fromRunnable(() -> {
            log.info("[MOCK EMAIL] To: {}, Subject: {}, Content: {}", recipient, subject, content);
            // Integrate with Email Service (e.g., AWS SES, SendGrid)
        });
    }
}
