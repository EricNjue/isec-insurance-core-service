package com.isec.platform.modules.notifications.service;

import com.isec.platform.modules.notifications.dto.SmsSendResult;
import com.isec.platform.modules.notifications.messaging.AfricasTalkingSmsClient;
import com.isec.platform.modules.notifications.model.SmsMessage;
import com.isec.platform.modules.notifications.model.SmsRecipientResult;
import com.isec.platform.modules.notifications.repository.SmsMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmsService {

    private final AfricasTalkingSmsClient smsClient;
    private final SmsMessageRepository smsMessageRepository;

    @Transactional
    public void sendSms(String to, String message) {
        log.info("Processing SMS request to: {}", to);
        String validatedTo = validateAndFormatPhoneNumber(to);
        validateMessage(message);

        log.debug("Sending SMS via provider to: {}", validatedTo);
        smsClient.sendSms(validatedTo, message)
                .doOnNext(result -> {
                    log.info("SMS successfully sent to: {}. Status: {}, MessageId: {}", 
                            validatedTo, result.isOverallSuccess() ? "Sent" : "Failed", 
                            result.getRecipients().isEmpty() ? "N/A" : result.getRecipients().get(0).getMessageId());
                    persistSmsResult(validatedTo, message, result);
                })
                .doOnError(error -> log.error("Failed to process SMS to {}: {}", validatedTo, error.getMessage()))
                .subscribe();
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

    private void persistSmsResult(String to, String message, SmsSendResult result) {
        log.debug("Persisting SMS result for: {}", to);
        String providerRequestId = result.getRecipients().isEmpty() ? null : result.getRecipients().get(0).getMessageId();

        SmsMessage smsMessage = SmsMessage.builder()
                .to(to)
                .message(message)
                .provider("AfricasTalking")
                .providerRequestId(providerRequestId)
                .statusSummary(result.getSummaryMessage())
                .build();

        List<SmsRecipientResult> recipientResults = result.getRecipients().stream()
                .map(r -> SmsRecipientResult.builder()
                        .smsMessage(smsMessage)
                        .number(r.getNumber())
                        .status(r.getStatus())
                        .statusCode(r.getStatusCode())
                        .messageId(r.getMessageId())
                        .cost(r.getCost())
                        .build())
                .collect(Collectors.toList());

        smsMessage.setRecipientResults(recipientResults);
        smsMessageRepository.save(smsMessage);
        log.info("SMS record persisted with ID: {} and providerRequestId: {}", smsMessage.getId(), providerRequestId);
    }
}
