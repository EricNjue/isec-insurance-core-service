package com.isec.platform.modules.integrations.mpesa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.modules.integrations.mpesa.config.MpesaConfig;
import com.isec.platform.modules.integrations.mpesa.domain.MpesaRequestLog;
import com.isec.platform.modules.integrations.mpesa.dto.MpesaDtos;
import com.isec.platform.modules.integrations.mpesa.repository.MpesaRequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class MpesaOAuthService {

    private final MpesaConfig mpesaConfig;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final WebClient.Builder webClientBuilder;
    private final MpesaRequestLogRepository logRepository;
    private final ObjectMapper objectMapper;

    private static final String ACCESS_TOKEN_CACHE_KEY = "mpesa_access_token";

    public Mono<String> getAccessToken() {
        return redisTemplate.opsForValue().get(ACCESS_TOKEN_CACHE_KEY)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("M-PESA Access Token expired or not found. Fetching new token...");
                    return fetchNewToken()
                            .flatMap(response -> {
                                if (response != null && response.getAccessToken() != null) {
                                    long expiresIn = Long.parseLong(response.getExpiresIn());
                                    log.info("M-PESA Access Token fetched successfully. Expires in {} seconds", expiresIn);
                                    // Cache token, expiring it slightly earlier (60s) than Safaricom to avoid race conditions
                                    return redisTemplate.opsForValue().set(
                                            ACCESS_TOKEN_CACHE_KEY,
                                            response.getAccessToken(),
                                            Duration.ofSeconds(expiresIn - 60)
                                    ).thenReturn(response.getAccessToken());
                                }
                                return Mono.error(new RuntimeException("Failed to generate M-PESA OAuth token"));
                            });
                }));
    }

    private Mono<MpesaDtos.OAuthResponse> fetchNewToken() {
        log.info("Generating new M-PESA OAuth token from Safaricom");
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (mpesaConfig.getConsumerKey() + ":" + mpesaConfig.getConsumerSecret()).getBytes()
        );

        return webClientBuilder.build()
                .get()
                .uri(mpesaConfig.getOauthUrl() + "?grant_type=client_credentials")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(MpesaDtos.OAuthResponse.class)
                .flatMap(response -> saveLog(response).thenReturn(response))
                .doOnNext(response -> log.info("M-PESA OAuth token generated successfully"))
                .doOnError(error -> log.error("Failed to fetch M-PESA OAuth token: {}", error.getMessage()));
    }

    private Mono<Void> saveLog(MpesaDtos.OAuthResponse response) {
        try {
            MpesaRequestLog logEntry = MpesaRequestLog.builder()
                    .requestType("OAUTH")
                    .requestPayload("grant_type=client_credentials")
                    .responsePayload(objectMapper.writeValueAsString(response))
                    .build();
            return logRepository.save(logEntry).then();
        } catch (Exception e) {
            log.error("Failed to save M-PESA OAuth request log", e);
            return Mono.empty();
        }
    }
}
