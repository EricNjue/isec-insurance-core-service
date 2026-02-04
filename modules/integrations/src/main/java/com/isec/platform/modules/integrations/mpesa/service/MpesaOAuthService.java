package com.isec.platform.modules.integrations.mpesa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.modules.integrations.mpesa.config.MpesaConfig;
import com.isec.platform.modules.integrations.mpesa.domain.MpesaRequestLog;
import com.isec.platform.modules.integrations.mpesa.dto.MpesaDtos;
import com.isec.platform.modules.integrations.mpesa.repository.MpesaRequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class MpesaOAuthService {

    private final MpesaConfig mpesaConfig;
    private final StringRedisTemplate redisTemplate;
    private final WebClient.Builder webClientBuilder;
    private final MpesaRequestLogRepository logRepository;
    private final ObjectMapper objectMapper;

    private static final String ACCESS_TOKEN_CACHE_KEY = "mpesa_access_token";

    public String getAccessToken() {
        String cachedToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_CACHE_KEY);
        if (cachedToken != null) {
            log.debug("M-PESA Access Token retrieved from cache");
            return cachedToken;
        }

        synchronized (this) {
            cachedToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_CACHE_KEY);
            if (cachedToken != null) {
                log.debug("M-PESA Access Token retrieved from cache (sync)");
                return cachedToken;
            }

            log.info("M-PESA Access Token expired or not found. Fetching new token...");
            MpesaDtos.OAuthResponse response = fetchNewToken();
            if (response != null && response.getAccessToken() != null) {
                long expiresIn = Long.parseLong(response.getExpiresIn());
                log.info("M-PESA Access Token fetched successfully. Expires in {} seconds", expiresIn);
                // Cache token, expiring it slightly earlier (60s) than Safaricom to avoid race conditions
                redisTemplate.opsForValue().set(
                        ACCESS_TOKEN_CACHE_KEY,
                        response.getAccessToken(),
                        Duration.ofSeconds(expiresIn - 60)
                );
                return response.getAccessToken();
            }
        }
        log.error("Failed to generate M-PESA OAuth token");
        throw new RuntimeException("Failed to generate M-PESA OAuth token");
    }

    private MpesaDtos.OAuthResponse fetchNewToken() {
        log.info("Generating new M-PESA OAuth token");
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (mpesaConfig.getConsumerKey() + ":" + mpesaConfig.getConsumerSecret()).getBytes()
        );

        MpesaDtos.OAuthResponse response = webClientBuilder.build()
                .get()
                .uri(mpesaConfig.getOauthUrl() + "?grant_type=client_credentials")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(MpesaDtos.OAuthResponse.class)
                .block();

        saveLog(response);
        return response;
    }

    private void saveLog(MpesaDtos.OAuthResponse response) {
        try {
            MpesaRequestLog logEntry = MpesaRequestLog.builder()
                    .requestType("OAUTH")
                    .requestPayload("grant_type=client_credentials")
                    .responsePayload(objectMapper.writeValueAsString(response))
                    .build();
            logRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to save M-PESA OAuth request log", e);
        }
    }
}
