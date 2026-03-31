package com.isec.platform.modules.integrations.sanlam.adapter;

import com.isec.platform.common.cache.CachingConfig;
import com.isec.platform.modules.integrations.common.adapter.InsuranceIntegrationAdapter;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckRequest;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckResponse;
import com.isec.platform.modules.integrations.sanlam.client.SanlamClient;
import com.isec.platform.modules.integrations.sanlam.dto.SanlamDoubleInsuranceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SanlamIntegrationAdapter implements InsuranceIntegrationAdapter {

    private final SanlamClient sanlamClient;
    private final CacheManager cacheManager;

    @Override
    public String getCompanyCode() {
        return "SANLAM";
    }

    @Override
    public DoubleInsuranceCheckResponse checkDoubleInsurance(DoubleInsuranceCheckRequest request) {
        String cacheKey = String.format("%s-%s", request.getRegistrationNumber(), request.getChassisNumber());
        
        return Optional.ofNullable(cacheManager.getCache(CachingConfig.SANLAM_DOUBLE_INSURANCE_CACHE))
                .map(cache -> {
                    DoubleInsuranceCheckResponse cachedResponse = cache.get(cacheKey, DoubleInsuranceCheckResponse.class);
                    if (cachedResponse != null) {
                        log.info("Cache hit for Sanlam double insurance check. Key: {}", cacheKey);
                        return cachedResponse;
                    }
                    
                    log.info("Cache miss for Sanlam double insurance check. Fetching from API. Key: {}", cacheKey);
                    DoubleInsuranceCheckResponse apiResponse = fetchDoubleInsuranceFromApi(request);
                    if (apiResponse != null) {
                        cache.put(cacheKey, apiResponse);
                    }
                    return apiResponse;
                })
                .orElseGet(() -> {
                    log.warn("Cache '{}' not found. Calling API directly.", CachingConfig.SANLAM_DOUBLE_INSURANCE_CACHE);
                    return fetchDoubleInsuranceFromApi(request);
                });
    }

    private DoubleInsuranceCheckResponse fetchDoubleInsuranceFromApi(DoubleInsuranceCheckRequest request) {
        log.info("Starting double insurance API call for Sanlam. Registration: {}", request.getRegistrationNumber());
        try {
            SanlamDoubleInsuranceResponse response = sanlamClient.checkDoubleInsurance(
                    request.getRegistrationNumber(),
                    request.getChassisNumber()
            );

            log.info("Sanlam API response received: status={}, message={}", response.getStatus(), response.getMessage());

            boolean hasDuplicate = "double".equalsIgnoreCase(response.getStatus());
            
            DoubleInsuranceCheckResponse canonicalResponse = DoubleInsuranceCheckResponse.builder()
                    .hasDuplicate(hasDuplicate)
                    .status(response.getStatus())
                    .message(response.getMessage())
                    .build();

            if (hasDuplicate && response.getEvidence() != null) {
                log.info("Duplicate cover found for vehicle {}. Insurer: {}, Expiry: {}", 
                        request.getRegistrationNumber(), response.getEvidence().getInsurer(), response.getEvidence().getCoverEndDate());
                canonicalResponse.setDetails(DoubleInsuranceCheckResponse.DuplicateDetails.builder()
                        .insurer(response.getEvidence().getInsurer())
                        .policyNumber(response.getEvidence().getPolicyNumber())
                        .certificateNumber(response.getEvidence().getCertificateNumber())
                        .expiryDate(response.getEvidence().getCoverEndDate())
                        .status(response.getEvidence().getCertificateStatus())
                        .build());
            } else {
                log.info("No active duplicate cover found for vehicle {} via Sanlam check", request.getRegistrationNumber());
            }

            return canonicalResponse;
        } catch (Exception e) {
            log.error("Error during Sanlam double insurance check: {}", e.getMessage(), e);
            throw e;
        }
    }
}
