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
        validateInput(to, message);

        smsClient.sendSms(to, message)
                .doOnNext(result -> persistSmsResult(to, message, result))
                .subscribe();
    }

    private void validateInput(String to, String message) {
        if (to == null || !to.startsWith("+")) {
            throw new IllegalArgumentException("Phone number must be in E.164 format (+...)");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
    }

    private void persistSmsResult(String to, String message, SmsSendResult result) {
        SmsMessage smsMessage = SmsMessage.builder()
                .to(to)
                .message(message)
                .provider("AfricasTalking")
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
    }
}
