package com.isec.platform.modules.integrations.sanlam.adapter;

import com.isec.platform.common.cache.CachingConfig;
import com.isec.platform.modules.integrations.common.adapter.InsuranceIntegrationAdapter;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckRequest;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckResponse;
import com.isec.platform.modules.integrations.common.dto.referencedata.ReferenceDataItem;
import com.isec.platform.modules.integrations.common.enums.ReferenceCategory;
import com.isec.platform.modules.integrations.sanlam.client.SanlamClient;
import com.isec.platform.modules.integrations.sanlam.dto.SanlamDependentReferenceDataResponse;
import com.isec.platform.modules.integrations.sanlam.dto.SanlamDoubleInsuranceResponse;
import com.isec.platform.modules.integrations.sanlam.dto.SanlamMasterReferenceDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private static final Map<String, ReferenceCategory> LABEL_TO_CATEGORY = Map.of(
            "Policy Type", ReferenceCategory.POLICY_TYPE,
            "Cover Type", ReferenceCategory.COVER_TYPE,
            "Body Type", ReferenceCategory.BODY_TYPE,
            "Vehicle Usage (Type)", ReferenceCategory.VEHICLE_USAGE,
            "Vehicle Make", ReferenceCategory.VEHICLE_MAKE,
            "Vehicle Model", ReferenceCategory.VEHICLE_MODEL,
            "City", ReferenceCategory.CITY,
            "Branch", ReferenceCategory.BRANCH
    );

    private static final Map<ReferenceCategory, String> CATEGORY_TO_LABEL = LABEL_TO_CATEGORY.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    @Override
    public Mono<Map<ReferenceCategory, List<ReferenceDataItem>>> fetchMasterReferenceData(String productCode) {
        log.info("SanlamAdapter: Fetching master reference data for product code: {}", productCode);
        return sanlamClient.fetchMasterReferenceData(productCode)
            .map(response -> {
                if (response == null || response.getData() == null) {
                    log.warn("SanlamAdapter: Received empty or null master reference data response");
                    return Collections.emptyMap();
                }

                Map<ReferenceCategory, List<ReferenceDataItem>> result = new HashMap<>();
                response.getData().forEach((groupId, categoryMap) -> {
                    categoryMap.forEach((label, items) -> {
                        ReferenceCategory category = LABEL_TO_CATEGORY.get(label);
                        if (category != null) {
                            List<ReferenceDataItem> canonicalItems = items.stream()
                                    .map(this::toCanonicalItem)
                                    .collect(Collectors.toList());
                            result.merge(category, canonicalItems, (oldList, newList) -> {
                                List<ReferenceDataItem> merged = new ArrayList<>(oldList);
                                merged.addAll(newList);
                                return merged;
                            });
                        }
                    });
                });
                return result;
            });
    }

    @Override
    public Mono<List<ReferenceDataItem>> fetchDependentReferenceData(String productCode,
                                                              ReferenceCategory parentCategory,
                                                              String parentValue,
                                                              ReferenceCategory childCategory) {
        String parentLabel = CATEGORY_TO_LABEL.get(parentCategory);
        String childLabel = CATEGORY_TO_LABEL.get(childCategory);

        log.info("SanlamAdapter: Fetching dependent data. Product: {}, Parent: {} ({}) -> Child: {} ({})", 
                productCode, parentCategory, parentLabel, childCategory, childLabel);

        if (parentLabel == null || childLabel == null) {
            log.warn("SanlamAdapter: Unsupported category mapping for dependent lookup: {} -> {}", parentCategory, childCategory);
            return Mono.just(Collections.emptyList());
        }

        return sanlamClient.fetchDependentReferenceData(parentLabel, parentValue, childLabel)
            .map(response -> {
                if (response == null || response.getData() == null || !response.getData().containsKey(childLabel)) {
                    log.warn("SanlamAdapter: No dependent data found for child label: {}", childLabel);
                    return Collections.emptyList();
                }

                return response.getData().get(childLabel).stream()
                        .map(this::toCanonicalItem)
                        .collect(Collectors.toList());
            });
    }

    private ReferenceDataItem toCanonicalItem(SanlamMasterReferenceDataResponse.SanlamReferenceDataItem item) {
        return ReferenceDataItem.builder()
                .code(item.getValueCode())
                .label(item.getValue())
                .sourceId(item.getStrAttrSysId())
                .build();
    }

    @Override
    public Mono<DoubleInsuranceCheckResponse> checkDoubleInsurance(DoubleInsuranceCheckRequest request) {
        String cacheKey = String.format("%s-%s", request.getRegistrationNumber(), request.getChassisNumber());
        
        Cache cache = cacheManager.getCache(CachingConfig.SANLAM_DOUBLE_INSURANCE_CACHE);
        if (cache != null) {
            DoubleInsuranceCheckResponse cachedResponse = cache.get(cacheKey, DoubleInsuranceCheckResponse.class);
            if (cachedResponse != null) {
                log.info("Cache hit for Sanlam double insurance check. Key: {}", cacheKey);
                return Mono.just(cachedResponse);
            }
        }
        
        log.info("Cache miss or no cache for Sanlam double insurance check. Fetching from API. Key: {}", cacheKey);
        return fetchDoubleInsuranceFromApi(request)
                .doOnNext(apiResponse -> {
                    if (cache != null && apiResponse != null) {
                        cache.put(cacheKey, apiResponse);
                    }
                });
    }

    private Mono<DoubleInsuranceCheckResponse> fetchDoubleInsuranceFromApi(DoubleInsuranceCheckRequest request) {
        log.info("Starting double insurance API call for Sanlam. Registration: {}", request.getRegistrationNumber());
        return sanlamClient.checkDoubleInsurance(
                request.getRegistrationNumber(),
                request.getChassisNumber()
        ).map(response -> {
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
        }).doOnError(error -> log.error("Error during Sanlam double insurance check: {}", error.getMessage()));
    }
}
