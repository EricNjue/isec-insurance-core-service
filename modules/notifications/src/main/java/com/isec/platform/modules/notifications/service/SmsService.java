package com.isec.platform.modules.notifications.service;

import com.isec.platform.modules.notifications.dto.SmsSendResult;
import com.isec.platform.modules.notifications.messaging.AfricasTalkingSmsClient;
import com.isec.platform.modules.notifications.model.SmsMessage;
import com.isec.platform.modules.notifications.model.SmsRecipientResult;
import com.isec.platform.modules.notifications.repository.SmsMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.isec.platform.modules.notifications.repository.SmsRecipientResultRepository;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmsService {

    private final AfricasTalkingSmsClient smsClient;
    private final SmsMessageRepository smsMessageRepository;
    private final SmsRecipientResultRepository smsRecipientResultRepository;

    public Mono<Void> sendSms(String to, String message) {
        log.info("Processing SMS request to: {}", to);
        try {
            String validatedTo = validateAndFormatPhoneNumber(to);
            validateMessage(message);

            log.debug("Sending SMS via provider to: {}", validatedTo);
            return smsClient.sendSms(validatedTo, message)
                    .flatMap(result -> {
                        log.info("SMS successfully sent to: {}. Status: {}, MessageId: {}", 
                                validatedTo, result.isOverallSuccess() ? "Sent" : "Failed", 
                                result.getRecipients().isEmpty() ? "N/A" : result.getRecipients().get(0).getMessageId());
                        return persistSmsResult(validatedTo, message, result);
                    })
                    .doOnError(error -> log.error("Failed to process SMS to {}: {}", validatedTo, error.getMessage()))
                    .then();
        } catch (Exception e) {
            log.error("Validation failed for SMS to {}: {}", to, e.getMessage());
            return Mono.error(e);
        }
    }

    private String validateAndFormatPhoneNumber(String to) {
        if (to == null || to.isBlank()) {
            log.warn("Validation failed: Phone number is empty");
            throw new IllegalArgumentException("Phone number cannot be empty");
        }

        String formatted = to.trim();
        if (!formatted.startsWith("+")) {
            formatted = "+" + formatted;
        }

        // Basic E.164 validation: + followed by 7 to 15 digits
        if (!formatted.matches("^\\+[1-9]\\d{6,14}$")) {
            log.warn("Validation failed: Invalid phone number format: {}", formatted);
            throw new IllegalArgumentException("Invalid phone number format. Must be E.164 (+ followed by 7-15 digits)");
        }

        return formatted;
    }

    private void validateMessage(String message) {
        if (message == null || message.isBlank()) {
            log.warn("Validation failed: Message content is empty");
            throw new IllegalArgumentException("Message content cannot be empty");
        }
    }

    private Mono<Void> persistSmsResult(String to, String message, SmsSendResult result) {
        log.debug("Persisting SMS result for: {}", to);
        String providerRequestId = result.getRecipients().isEmpty() ? null : result.getRecipients().get(0).getMessageId();

        SmsMessage smsMessage = SmsMessage.builder()
                .recipientTo(to)
                .messageContent(message)
                .provider("AfricasTalking")
                .providerRequestId(providerRequestId)
                .statusSummary(result.getSummaryMessage())
                .createdAt(LocalDateTime.now())
                .build();

        return smsMessageRepository.save(smsMessage)
                .flatMap(savedMessage -> {
                    log.info("SMS record persisted with ID: {} and providerRequestId: {}", savedMessage.getId(), providerRequestId);
                    
                    var results = result.getRecipients().stream()
                            .map(r -> SmsRecipientResult.builder()
                                    .smsMessageId(savedMessage.getId())
                                    .number(r.getNumber())
                                    .status(r.getStatus())
                                    .statusCode(r.getStatusCode())
                                    .messageId(r.getMessageId())
                                    .cost(r.getCost())
                                    .createdAt(LocalDateTime.now())
                                    .build())
                            .toList();
                    
                    return smsRecipientResultRepository.saveAll(results).then();
                });
    }
}
