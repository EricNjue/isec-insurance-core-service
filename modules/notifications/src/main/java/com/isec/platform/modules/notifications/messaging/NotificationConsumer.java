package com.isec.platform.modules.notifications.messaging;

import com.isec.platform.common.idempotency.service.IdempotencyService;
import com.isec.platform.messaging.RabbitMQConfig;
import com.isec.platform.messaging.events.NotificationSendEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final IdempotencyService idempotencyService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_SEND_QUEUE)
    public void handleNotificationSend(NotificationSendEvent event) {
        log.info("Received notification send event: {} for recipient: {}", event.getEventId(), event.getRecipient());

        if (idempotencyService.isDuplicate(event.getEventId())) {
            log.info("Duplicate notification event detected, skipping: {}", event.getEventId());
            return;
        }

        try {
            if ("SMS".equalsIgnoreCase(event.getChannel())) {
                sendSms(event.getRecipient(), event.getContent());
            } else {
                sendEmail(event.getRecipient(), event.getSubject(), event.getContent());
            }
            log.info("Successfully sent {} notification for event: {}", event.getChannel(), event.getEventId());
        } catch (Exception e) {
            log.error("Failed to send notification for event: {}. Reason: {}", event.getEventId(), e.getMessage());
            // In real world, we might want to retry specifically for transient network issues
            throw e;
        }
    }

    private void sendSms(String recipient, String content) {
        log.info("[MOCK SMS] To: {}, Content: {}", recipient, content);
        // Integrate with SMS Gateway (e.g., Twilio, Africa's Talking)
    }

    private void sendEmail(String recipient, String subject, String content) {
        log.info("[MOCK EMAIL] To: {}, Subject: {}, Content: {}", recipient, subject, content);
        // Integrate with Email Service (e.g., AWS SES, SendGrid)
    }
}
