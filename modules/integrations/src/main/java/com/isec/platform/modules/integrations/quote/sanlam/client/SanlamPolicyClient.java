package com.isec.platform.modules.integrations.quote.sanlam.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.modules.integrations.quote.sanlam.config.SanlamQuoteProperties;
import com.isec.platform.modules.integrations.quote.sanlam.dto.SanlamDraftQuoteResponse;
import com.isec.platform.modules.integrations.quote.sanlam.dto.SanlamEmailRequest;
import com.isec.platform.modules.integrations.quote.sanlam.dto.SanlamEmailResponse;
import com.isec.platform.modules.integrations.quote.sanlam.dto.SanlamUpdateDraftQuoteRequest;
import com.isec.platform.modules.integrations.sanlam.client.SanlamClient;
import com.isec.platform.reactive.infra.http.HttpClientOptions;
import com.isec.platform.reactive.infra.http.ReactiveHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@Slf4j
@RequiredArgsConstructor
public class SanlamPolicyClient {

    private final ReactiveHttpClient httpClient;
    private final SanlamClient sanlamAuthClient;
    private final SanlamQuoteProperties properties;
    private final ObjectMapper objectMapper;

    public Mono<SanlamDraftQuoteResponse> updateDraftQuote(Long draftQuoteSysId, SanlamUpdateDraftQuoteRequest request) {
        try {
            log.info("SanlamPolicyClient: Updating draft quote. SysId: {}, Payload: {}", 
                    draftQuoteSysId, objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            log.info("SanlamPolicyClient: Updating draft quote. SysId: {}", draftQuoteSysId);
        }

        return sanlamAuthClient.getAccessToken()
                .flatMap(token -> {
                    String url = properties.getBaseUrl() + properties.getUpdateDraftQuotePath()
                            .replace("{draftQuoteSysId}", String.valueOf(draftQuoteSysId));
                    
                    HttpClientOptions options = HttpClientOptions.builder()
                            .timeout(properties.getTimeout())
                            .retrySpec(createRetrySpec())
                            .headers(headers -> {
                                headers.setBearerAuth(token);
                                headers.setContentType(MediaType.APPLICATION_JSON);
                            })
                            .build();

                    return httpClient.put(url, request, SanlamDraftQuoteResponse.class, options);
                })
                .doOnNext(response -> {
                    try {
                        log.info("SanlamPolicyClient: Draft quote updated. QuotSysId: {}, Response: {}", 
                                response.getQuotSysId(), objectMapper.writeValueAsString(response));
                    } catch (Exception e) {
                        log.info("SanlamPolicyClient: Draft quote updated. QuotSysId: {}", response.getQuotSysId());
                    }
                })
                .doOnError(error -> log.error("SanlamPolicyClient: Failed to update draft quote. Error: {}", error.getMessage()));
    }

    public Mono<SanlamEmailResponse> sendDocuments(SanlamEmailRequest request) {
        log.info("SanlamPolicyClient: Sending insurance documents. QuotSysId: {}", request.getQuotSysId());

        return sanlamAuthClient.getAccessToken()
                .flatMap(token -> {
                    String url = properties.getBaseUrl() + properties.getSendDocumentsEmailPath();
                    
                    HttpClientOptions options = HttpClientOptions.builder()
                            .timeout(properties.getTimeout())
                            .retrySpec(createRetrySpec())
                            .headers(headers -> {
                                headers.setBearerAuth(token);
                                headers.setContentType(MediaType.APPLICATION_JSON);
                            })
                            .build();

                    return httpClient.post(url, request, SanlamEmailResponse.class, options);
                })
                .doOnNext(response -> log.info("SanlamPolicyClient: Email response: {}", response.getMessage()))
                .doOnError(error -> log.error("SanlamPolicyClient: Failed to send documents. Error: {}", error.getMessage()));
    }

    private Retry createRetrySpec() {
        if (!properties.getRetry().isEnabled()) {
            return Retry.max(0);
        }
        return Retry.backoff(properties.getRetry().getMaxAttempts(), properties.getRetry().getBackoff())
                .filter(this::isRetryable);
    }

    private boolean isRetryable(Throwable throwable) {
        return true; 
    }
}
