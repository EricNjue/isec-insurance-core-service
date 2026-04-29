package com.isec.platform.modules.integrations.premium.sanlam.client;

import com.isec.platform.modules.integrations.premium.sanlam.config.SanlamPremiumCalculationProperties;
import com.isec.platform.modules.integrations.premium.sanlam.dto.SanlamCalculatePremiumRequest;
import com.isec.platform.modules.integrations.premium.sanlam.dto.SanlamCalculatePremiumResponse;
import com.isec.platform.modules.integrations.sanlam.client.SanlamClient;
import com.isec.platform.reactive.infra.http.HttpClientOptions;
import com.isec.platform.reactive.infra.http.ReactiveHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@Slf4j
@RequiredArgsConstructor
public class SanlamPremiumCalculationClient {

    private final ReactiveHttpClient httpClient;
    private final SanlamClient sanlamAuthClient;
    private final SanlamPremiumCalculationProperties properties;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public Mono<SanlamCalculatePremiumResponse> calculatePremium(SanlamCalculatePremiumRequest request) {
        String url = properties.getBaseUrl() + properties.getCalculatePremiumPath();
        try {
            log.info("Calling Sanlam Premium Calculation API: {} with payload: {}", url, objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            log.info("Calling Sanlam Premium Calculation API: {}", url);
        }

        return sanlamAuthClient.getAccessToken()
                .flatMap(token -> {
                    HttpClientOptions options = HttpClientOptions.builder()
                            .timeout(properties.getTimeout())
                            .retrySpec(properties.getRetry().isEnabled() ? 
                                    Retry.backoff(properties.getRetry().getMaxAttempts(), properties.getRetry().getBackoff()) : 
                                    Retry.max(0))
                            .headers(headers -> {
                                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                                headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
                            })
                            .build();

                    return httpClient.post(url, request, SanlamCalculatePremiumResponse.class, options);
                })
                .doOnNext(response -> {
                    try {
                        log.info("Received response from Sanlam Premium Calculation API: {}", objectMapper.writeValueAsString(response));
                    } catch (Exception e) {
                        log.info("Successfully received response from Sanlam Premium Calculation API");
                    }
                })
                .doOnError(error -> log.error("Error calling Sanlam Premium Calculation API: {}", error.getMessage()));
    }
}
