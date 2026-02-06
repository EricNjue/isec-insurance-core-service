package com.isec.platform.modules.notifications.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.modules.notifications.model.SmsDeliveryReport;
import com.isec.platform.modules.notifications.model.SmsRecipientResult;
import com.isec.platform.modules.notifications.repository.SmsDeliveryReportRepository;
import com.isec.platform.modules.notifications.repository.SmsRecipientResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryReportService {

    private final SmsDeliveryReportRepository deliveryReportRepository;
    private final SmsRecipientResultRepository recipientResultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void handleFormPayload(Map<String, String> formParams) {
        String messageId = formParams.get("id");
        String phoneNumber = formParams.get("phoneNumber");
        String status = formParams.get("status");

        if (messageId == null || messageId.isBlank() || phoneNumber == null || phoneNumber.isBlank() || status == null || status.isBlank()) {
            log.warn("Delivery report payload missing required fields: id, phoneNumber, status");
            // We still return 200 at controller level; here we just log and exit
            return;
        }

        String rawJson = safeToJson(formParams);

        // Idempotent upsert by messageId
        SmsDeliveryReport report = deliveryReportRepository.findByMessageId(messageId)
            .map(existing -> {
                existing.setPhoneNumber(phoneNumber);
                existing.setStatus(status);
                existing.setFailureReason(formParams.get("failureReason"));
                existing.setRetryCount(parseInt(formParams.get("retryCount")));
                existing.setNetworkCode(formParams.get("networkCode"));
                existing.setRawPayload(rawJson);
                return existing;
            })
            .orElseGet(() -> SmsDeliveryReport.builder()
                .messageId(messageId)
                .phoneNumber(phoneNumber)
                .status(status)
                .failureReason(formParams.get("failureReason"))
                .retryCount(parseInt(formParams.get("retryCount")))
                .networkCode(formParams.get("networkCode"))
                .rawPayload(rawJson)
                .build()
            );

        deliveryReportRepository.save(report);

        // Correlate with recipient result and update delivery fields
        recipientResultRepository.findByMessageId(messageId).ifPresent(recipient -> {
            recipient.setDeliveryStatus(status);
            recipient.setDeliveryFailureReason(formParams.get("failureReason"));
            recipient.setDeliveryReportedAt(LocalDateTime.now());
        });
    }

    private Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String safeToJson(Map<String, String> formParams) {
        try {
            return objectMapper.writeValueAsString(formParams);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
