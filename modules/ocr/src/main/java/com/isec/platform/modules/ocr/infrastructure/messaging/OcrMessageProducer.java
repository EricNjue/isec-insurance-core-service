package com.isec.platform.modules.ocr.infrastructure.messaging;

import com.isec.platform.messaging.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OcrMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publishDocumentSubmitted(UUID documentId, UUID tenantId, String documentType, String s3Url, String sha256) {
        Map<String, Object> payload = Map.of(
                "documentId", documentId,
                "tenantId", tenantId,
                "documentType", documentType,
                "s3Url", s3Url,
                "sha256", sha256
        );
        log.info("Publishing OCR document submitted: {}", payload);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.OCR_DOCUMENT_SUBMITTED_RK, payload);
    }
}
