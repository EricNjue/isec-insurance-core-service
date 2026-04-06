package com.isec.platform.modules.integrations.sanlam.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.isec.platform.modules.integrations.sanlam.dto.SanlamDoubleInsuranceResponse;
import com.isec.platform.modules.integrations.sanlam.dto.SanlamMasterReferenceDataResponse;
import com.isec.platform.modules.integrations.sanlam.dto.SanlamDependentReferenceDataResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class SanlamClient {

    private final WebClient.Builder webClientBuilder;
    private final StringRedisTemplate redisTemplate;

    @Value("${integrations.sanlam.base-url}")
    private String baseUrl;

    @Value("${integrations.sanlam.username}")
    private String username;

    @Value("${integrations.sanlam.password}")
    private String password;

    @Value("${integrations.sanlam.token-cache-key:sanlam_access_token}")
    private String tokenCacheKey;

    @Value("${integrations.sanlam.token-expiry-buffer-minutes:5}")
    private int tokenExpiryBufferMinutes;

    @Value("${integrations.sanlam.reference-data.master-endpoint:/masters/product_lovs/{productCode}}")
    private String masterEndpoint;

    @Value("${integrations.sanlam.reference-data.dependent-endpoint:/masters/child_lov}")
    private String dependentEndpoint;

    public String getAccessToken() {
        String cachedToken = redisTemplate.opsForValue().get(tokenCacheKey);
        if (cachedToken != null) {
            log.debug("Sanlam access token retrieved from cache");
            return cachedToken;
        }

        synchronized (this) {
            cachedToken = redisTemplate.opsForValue().get(tokenCacheKey);
            if (cachedToken != null) {
                log.debug("Sanlam access token retrieved from cache (sync)");
                return cachedToken;
            }

            log.info("Sanlam access token expired or not found. Fetching new token from Sanlam...");
            AccessTokenResponse response = fetchNewToken().block();
            if (response != null && response.getAccessToken() != null) {
                // Sanlam tokens are valid for 24 hours (86400 seconds)
                // We cache for 24 hours minus buffer
                long expiresIn = 86400; // Default if not provided
                log.info("Sanlam access token fetched successfully. Caching for {} seconds (minus {} min buffer)", 
                        expiresIn, tokenExpiryBufferMinutes);
                redisTemplate.opsForValue().set(
                        tokenCacheKey,
                        response.getAccessToken(),
                        Duration.ofSeconds(expiresIn).minusMinutes(tokenExpiryBufferMinutes)
                );
                return response.getAccessToken();
            }
        }
        log.error("Failed to fetch Sanlam access token from {}", baseUrl);
        throw new RuntimeException("Failed to fetch Sanlam access token");
    }

    private Mono<AccessTokenResponse> fetchNewToken() {
        log.info("Requesting new Sanlam token for user: {}", username);
        return webClientBuilder.build()
                .post()
                .uri(baseUrl + "/auth/login/access-token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", username)
                        .with("password", password)
                        .with("grant_type", "password")
                        .with("scope", "")
                        .with("client_id", "")
                        .with("client_secret", ""))
                .retrieve()
                .bodyToMono(AccessTokenResponse.class)
                .doOnError(error -> log.error("Error fetching Sanlam token: {}", error.getMessage()));
    }

    public SanlamDoubleInsuranceResponse checkDoubleInsurance(String registrationNumber, String chassisNumber) {
        log.info("Calling Sanlam double insurance check for LPN: {}, chassis: {}", registrationNumber, chassisNumber);
        String token = getAccessToken();
        
        Map<String, String> body = new java.util.HashMap<>();
        body.put("registration_number", registrationNumber);
        if (chassisNumber != null && !chassisNumber.isEmpty()) {
            body.put("chassis_number", chassisNumber);
        }

        return webClientBuilder.build()
                .post()
                .uri(baseUrl + "/external_apis/dmvic/check-double-insurance")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(SanlamDoubleInsuranceResponse.class)
                .doOnNext(response -> log.info("Sanlam double insurance response: status={}, message={}", 
                        response.getStatus(), response.getMessage()))
                .doOnError(error -> log.error("Sanlam double insurance check failed: {}", error.getMessage()))
                .block();
    }

    public SanlamMasterReferenceDataResponse fetchMasterReferenceData(String productCode) {
        String path = masterEndpoint.replace("{productCode}", productCode);
        String fullUrl = baseUrl + path;
        log.info("SanlamClient: Fetching master reference data. URL: {}", fullUrl);
        
        String token = getAccessToken();

        Map<String, Map<String, List<SanlamMasterReferenceDataResponse.SanlamReferenceDataItem>>> rawResponse = webClientBuilder.build()
                .get()
                .uri(fullUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Map<String, List<SanlamMasterReferenceDataResponse.SanlamReferenceDataItem>>>>() {})
                .doOnNext(resp -> {
                    if (resp != null) {
                        log.info("SanlamClient: Master reference data fetched successfully. Groups found: {}", resp.keySet());
                    }
                })
                .doOnError(error -> log.error("SanlamClient: Failed to fetch master reference data from {}. Error: {}", fullUrl, error.getMessage()))
                .block();

        return new SanlamMasterReferenceDataResponse(rawResponse);
    }

    public SanlamDependentReferenceDataResponse fetchDependentReferenceData(String parentAttrName, String parentValue, String childAttrName) {
        log.info("SanlamClient: Fetching dependent reference data. Base URL: {}, Path: {}, Parent: {}={}, Child: {}", 
                baseUrl, dependentEndpoint, parentAttrName, parentValue, childAttrName);
        
        String token = getAccessToken();

        Map<String, List<SanlamMasterReferenceDataResponse.SanlamReferenceDataItem>> rawResponse = webClientBuilder.build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .scheme(baseUrl.split("://")[0])
                        .host(baseUrl.split("://")[1].split("/")[0])
                        .path(dependentEndpoint)
                        .queryParam("parent_attr_name", parentAttrName)
                        .queryParam("parent_value", parentValue)
                        .queryParam("child_attr_name", childAttrName)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, List<SanlamMasterReferenceDataResponse.SanlamReferenceDataItem>>>() {})
                .doOnNext(resp -> {
                    if (resp != null) {
                        log.info("SanlamClient: Dependent reference data fetched successfully. Child categories found: {}", resp.keySet());
                    }
                })
                .doOnError(error -> log.error("SanlamClient: Failed to fetch dependent reference data from {}. Error: {}", baseUrl + dependentEndpoint, error.getMessage()))
                .block();

        return new SanlamDependentReferenceDataResponse(rawResponse);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccessTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("token_type")
        private String tokenType;
    }
}
