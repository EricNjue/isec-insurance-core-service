package com.isec.platform.modules.notifications.messaging;

import com.isec.platform.modules.notifications.config.AfricasTalkingProperties;
import com.isec.platform.modules.notifications.dto.AfricasTalkingSmsResponse;
import com.isec.platform.modules.notifications.dto.SmsSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class AfricasTalkingSmsClient {

    private final AfricasTalkingProperties properties;
    private final WebClient.Builder webClientBuilder;

    public Mono<SmsSendResult> sendSms(String to, String message) {
        return sendSms(to, message, properties.getFrom());
    }

    public Mono<SmsSendResult> sendSms(String to, String message, String from) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", properties.getUsername());
        formData.add("to", to);
        formData.add("message", message);
        if (from != null && !from.isBlank()) {
            formData.add("from", from);
        }

        return webClientBuilder.build()
                .post()
                .uri(properties.getBaseUrl() + "/version1/messaging")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header("apiKey", properties.getApiKey())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(AfricasTalkingSmsResponse.class)
                .map(this::mapToInternalResult)
                .doOnNext(result -> log.info("SMS sent result: {}", result))
                .doOnError(error -> log.error("Failed to send SMS to {}: {}", to, error.getMessage()));
    }

    private SmsSendResult mapToInternalResult(AfricasTalkingSmsResponse response) {
        var smsData = response.getSMSMessageData();
        var recipients = smsData.getRecipients().stream()
                .map(r -> SmsSendResult.RecipientResult.builder()
                        .number(r.getNumber())
                        .status(r.getStatus())
                        .statusCode(r.getStatusCode())
                        .messageId(r.getMessageId())
                        .cost(r.getCost())
                        .build())
                .collect(Collectors.toList());

        boolean overallSuccess = recipients.stream()
                .allMatch(r -> r.getStatusCode() == 100 || r.getStatusCode() == 101 || r.getStatusCode() == 102);

        return SmsSendResult.builder()
                .summaryMessage(smsData.getMessage())
                .recipients(recipients)
                .overallSuccess(overallSuccess)
                .build();
    }
}
