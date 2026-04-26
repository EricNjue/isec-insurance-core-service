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

import java.time.LocalDateTime;
import java.util.Map;

import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryReportService {

    private final SmsDeliveryReportRepository deliveryReportRepository;
    private final SmsRecipientResultRepository recipientResultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<Void> handleFormPayload(Map<String, String> formParams) {
        String messageId = formParams.get("id");
        String phoneNumber = formParams.get("phoneNumber");
        String status = formParams.get("status");

        if (messageId == null || messageId.isBlank() || phoneNumber == null || phoneNumber.isBlank() || status == null || status.isBlank()) {
            log.warn("Delivery report payload missing required fields: id, phoneNumber, status");
            return Mono.empty();
        }

        String rawJson = safeToJson(formParams);

        return deliveryReportRepository.findByMessageId(messageId)
            .flatMap(existing -> {
                existing.setPhoneNumber(phoneNumber);
                existing.setStatus(status);
                existing.setFailureReason(formParams.get("failureReason"));
                existing.setRetryCount(parseInt(formParams.get("retryCount")));
                existing.setNetworkCode(formParams.get("networkCode"));
                existing.setRawPayload(rawJson);
                return deliveryReportRepository.save(existing);
            })
            .switchIfEmpty(Mono.defer(() -> {
                SmsDeliveryReport report = SmsDeliveryReport.builder()
                    .id(java.util.UUID.randomUUID())
                    .messageId(messageId)
                    .phoneNumber(phoneNumber)
                    .status(status)
                    .failureReason(formParams.get("failureReason"))
                    .retryCount(parseInt(formParams.get("retryCount")))
                    .networkCode(formParams.get("networkCode"))
                    .rawPayload(rawJson)
                    .receivedAt(LocalDateTime.now())
                    .build();
                return deliveryReportRepository.save(report);
            }))
            .flatMap(report -> recipientResultRepository.findByMessageId(messageId)
                .flatMap(recipient -> {
                    recipient.setDeliveryStatus(status);
                    recipient.setDeliveryFailureReason(formParams.get("failureReason"));
                    recipient.setDeliveryReportedAt(LocalDateTime.now());
                    return recipientResultRepository.save(recipient);
                }))
            .then();
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
