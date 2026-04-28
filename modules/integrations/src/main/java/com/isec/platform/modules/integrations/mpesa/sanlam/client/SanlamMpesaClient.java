package com.isec.platform.modules.integrations.mpesa.sanlam.client;

import com.isec.platform.modules.integrations.sanlam.client.SanlamClient;
import com.isec.platform.modules.integrations.mpesa.sanlam.dto.SanlamStkPushRequest;
import com.isec.platform.modules.integrations.mpesa.sanlam.dto.SanlamStkStatusRequest;
import com.isec.platform.modules.integrations.mpesa.sanlam.dto.SanlamStkPushResponse;
import com.isec.platform.modules.integrations.mpesa.sanlam.dto.SanlamStkStatusResponse;
import com.isec.platform.reactive.infra.http.HttpClientOptions;
import com.isec.platform.reactive.infra.http.ReactiveHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class SanlamMpesaClient {

    private final ReactiveHttpClient httpClient;
    private final SanlamClient sanlamClient;

    @Value("${integrations.mpesa.sanlam.base-url}")
    private String baseUrl;

    @Value("${integrations.mpesa.sanlam.stk-push-path}")
    private String stkPushPath;

    @Value("${integrations.mpesa.sanlam.stk-status-path}")
    private String stkStatusPath;

    @Value("${integrations.mpesa.sanlam.timeout:5s}")
    private Duration timeout;

    public Mono<SanlamStkPushResponse> initiateStkPush(SanlamStkPushRequest request) {
        return sanlamClient.getAccessToken()
                .flatMap(token -> {
                    String url = baseUrl + stkPushPath;
                    HttpClientOptions options = HttpClientOptions.builder()
                            .timeout(timeout)
                            .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                            .build();
                    
                    log.info("Initiating Sanlam M-Pesa STK Push for quote_ref: {}", request.getQuoteRef());
                    return httpClient.post(url, request, SanlamStkPushResponse.class, options)
                            .doOnNext(response -> log.info("Sanlam M-Pesa STK Push response for {}: {}", request.getQuoteRef(), response.getStatus()))
                            .doOnError(error -> log.error("Sanlam M-Pesa STK Push failed for {}: {}", request.getQuoteRef(), error.getMessage()));
                });
    }

    public Mono<SanlamStkStatusResponse> checkStkStatus(SanlamStkStatusRequest request) {
        return sanlamClient.getAccessToken()
                .flatMap(token -> {
                    String url = baseUrl + stkStatusPath;
                    HttpClientOptions options = HttpClientOptions.builder()
                            .timeout(timeout)
                            .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                            .build();

                    log.info("Checking Sanlam M-Pesa STK status for checkout_id: {}", request.getCheckoutId());
                    return httpClient.post(url, request, SanlamStkStatusResponse.class, options)
                            .doOnNext(response -> log.info("Sanlam M-Pesa STK status response for {}: {}", request.getCheckoutId(), response.getStatus()))
                            .doOnError(error -> log.error("Sanlam M-Pesa STK status check failed for {}: {}", request.getCheckoutId(), error.getMessage()));
                });
    }
}
