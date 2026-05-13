package com.isec.platform.modules.integrations.mpesa.sanlam.client;

import com.isec.platform.modules.integrations.mpesa.model.MpesaVerifyReceiptRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaVerifyReceiptResponse;
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

import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class SanlamMpesaClient {

    private final ReactiveHttpClient httpClient;
    private final SanlamClient sanlamClient;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Value("${integrations.mpesa.sanlam.base-url}")
    private String baseUrl;

    @Value("${integrations.mpesa.sanlam.stk-push-path}")
    private String stkPushPath;

    @Value("${integrations.mpesa.sanlam.stk-status-path}")
    private String stkStatusPath;

    @Value("${integrations.mpesa.sanlam.verify-receipt-path}")
    private String verifyReceiptPath;

    @Value("${integrations.mpesa.sanlam.timeout:5s}")
    private Duration timeout;

    public Mono<SanlamStkPushResponse> initiateStkPush(SanlamStkPushRequest request) {
        return sanlamClient.getAccessToken()
                .flatMap(token -> {
                    String url = baseUrl + stkPushPath;
                    HttpClientOptions options = HttpClientOptions.builder()
                            .timeout(timeout)
                            .retrySpec(Retry.backoff(3, Duration.ofSeconds(1)))
                            .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                            .build();
                    
                    try {
                        log.info("Initiating Sanlam M-Pesa STK Push for quote_ref: {} with payload: {}", 
                                request.getQuoteRef(), objectMapper.writeValueAsString(request));
                    } catch (Exception e) {
                        log.info("Initiating Sanlam M-Pesa STK Push for quote_ref: {}", request.getQuoteRef());
                    }
                    return httpClient.post(url, request, SanlamStkPushResponse.class, options)
                            .doOnNext(response -> {
                                try {
                                    log.info("Sanlam M-Pesa STK Push response for {}: {}", 
                                            request.getQuoteRef(), objectMapper.writeValueAsString(response));
                                } catch (Exception e) {
                                    log.info("Sanlam M-Pesa STK Push response for {}: {}", request.getQuoteRef(), response.getStatus());
                                }
                            })
                            .doOnError(error -> log.error("Sanlam M-Pesa STK Push failed for {}: {}", request.getQuoteRef(), error.getMessage()));
                });
    }

    public Mono<SanlamStkStatusResponse> checkStkStatus(SanlamStkStatusRequest request) {
        return sanlamClient.getAccessToken()
                .flatMap(token -> {
                    String url = baseUrl + stkStatusPath;
                    HttpClientOptions options = HttpClientOptions.builder()
                            .timeout(timeout)
                            .retrySpec(Retry.backoff(3, Duration.ofSeconds(1)))
                            .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                            .build();

                    try {
                        log.info("Checking Sanlam M-Pesa STK status for checkout_id: {} with payload: {}", 
                                request.getCheckoutId(), objectMapper.writeValueAsString(request));
                    } catch (Exception e) {
                        log.info("Checking Sanlam M-Pesa STK status for checkout_id: {}", request.getCheckoutId());
                    }
                    return httpClient.post(url, request, SanlamStkStatusResponse.class, options)
                            .doOnNext(response -> {
                                try {
                                    log.info("Sanlam M-Pesa STK status response for {}: {}", 
                                            request.getCheckoutId(), objectMapper.writeValueAsString(response));
                                } catch (Exception e) {
                                    log.info("Sanlam M-Pesa STK status response for {}: {}", request.getCheckoutId(), response.getStatus());
                                }
                            })
                            .doOnError(error -> log.error("Sanlam M-Pesa STK status check failed for {}: {}", request.getCheckoutId(), error.getMessage()));
                });
    }

    public Mono<MpesaVerifyReceiptResponse> verifyReceipt(MpesaVerifyReceiptRequest request) {
        return sanlamClient.getAccessToken()
                .flatMap(token -> {
                    String url = baseUrl + verifyReceiptPath;
                    HttpClientOptions options = HttpClientOptions.builder()
                            .timeout(timeout)
                            .retrySpec(Retry.backoff(3, Duration.ofSeconds(1)))
                            .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                            .build();

                    try {
                        log.info("Verifying Sanlam M-Pesa receipt for quote_ref: {}, receipt: {} with payload: {}", 
                                request.getQuoteRef(), request.getReceipt(), objectMapper.writeValueAsString(request));
                    } catch (Exception e) {
                        log.info("Verifying Sanlam M-Pesa receipt for quote_ref: {}, receipt: {}", request.getQuoteRef(), request.getReceipt());
                    }
                    return httpClient.post(url, request, MpesaVerifyReceiptResponse.class, options)
                            .doOnNext(response -> {
                                try {
                                    log.info("Sanlam M-Pesa receipt verification response for {}: {}", 
                                            request.getReceipt(), objectMapper.writeValueAsString(response));
                                } catch (Exception e) {
                                    log.info("Sanlam M-Pesa receipt verification response for {}: {}", request.getReceipt(), response.getStatus());
                                }
                            })
                            .doOnError(error -> log.error("Sanlam M-Pesa receipt verification failed for {}: {}", request.getReceipt(), error.getMessage()));
                });
    }
}
