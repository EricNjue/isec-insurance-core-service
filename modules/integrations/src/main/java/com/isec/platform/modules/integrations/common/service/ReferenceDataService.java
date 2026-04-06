package com.isec.platform.modules.integrations.common.service;

import com.isec.platform.common.cache.CachingConfig;
import com.isec.platform.modules.integrations.common.adapter.InsuranceIntegrationAdapter;
import com.isec.platform.modules.integrations.common.dto.referencedata.MasterReferenceDataResponse;
import com.isec.platform.modules.integrations.common.dto.referencedata.ReferenceDataItem;
import com.isec.platform.modules.integrations.common.dto.referencedata.SingleCategoryReferenceDataResponse;
import com.isec.platform.modules.integrations.common.dto.referencedata.DependentReferenceDataResponse;
import com.isec.platform.modules.integrations.common.enums.ReferenceCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReferenceDataService {

    private final Map<String, InsuranceIntegrationAdapter> integrationAdapters;
    private final CacheManager cacheManager;

    public MasterReferenceDataResponse getMasterReferenceData(String companyCode, String productCode) {
        log.info("Fetching all master reference data for company: {}, product: {}", companyCode, productCode);
        long startTime = System.currentTimeMillis();
        
        InsuranceIntegrationAdapter adapter = getAdapter(companyCode);
        Map<ReferenceCategory, List<ReferenceDataItem>> allCategories = fetchAllCategoriesWithCaching(adapter, companyCode, productCode);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Master reference data fetch completed for {}. Duration: {}ms. Categories found: {}", 
                companyCode, duration, allCategories.keySet());

        return MasterReferenceDataResponse.builder()
                .companyCode(companyCode.toUpperCase())
                .productCode(productCode)
                .categories(allCategories)
                .servedFromCache(true) // Simplified for now, could be more granular
                .lastRefreshedAt(OffsetDateTime.now())
                .build();
    }

    public SingleCategoryReferenceDataResponse getSingleCategoryReferenceData(String companyCode, String productCode, ReferenceCategory categoryKey) {
        log.info("Fetching single category reference data. Company: {}, Product: {}, Category: {}", 
                companyCode, productCode, categoryKey);
        long startTime = System.currentTimeMillis();

        InsuranceIntegrationAdapter adapter = getAdapter(companyCode);
        List<ReferenceDataItem> items = getCategoryItemsWithCaching(adapter, companyCode, productCode, categoryKey);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Single category fetch completed for {} - {}. Duration: {}ms. Items found: {}", 
                companyCode, categoryKey, duration, items.size());

        return SingleCategoryReferenceDataResponse.builder()
                .companyCode(companyCode.toUpperCase())
                .productCode(productCode)
                .categoryKey(categoryKey)
                .items(items)
                .servedFromCache(true)
                .lastRefreshedAt(OffsetDateTime.now())
                .build();
    }

    public DependentReferenceDataResponse getDependentReferenceData(String companyCode, String productCode,
                                                                   ReferenceCategory parentCategoryKey, String parentValue,
                                                                   ReferenceCategory childCategoryKey) {
        log.info("Fetching dependent reference data. Company: {}, Product: {}, Parent: {} ({}), Child: {}", 
                companyCode, productCode, parentCategoryKey, parentValue, childCategoryKey);
        long startTime = System.currentTimeMillis();

        InsuranceIntegrationAdapter adapter = getAdapter(companyCode);
        List<ReferenceDataItem> items = getDependentItemsWithCaching(adapter, companyCode, productCode, parentCategoryKey, parentValue, childCategoryKey);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Dependent category fetch completed for {} - {} ({} -> {}). Duration: {}ms. Items found: {}", 
                companyCode, productCode, parentCategoryKey, childCategoryKey, duration, items.size());

        return DependentReferenceDataResponse.builder()
                .companyCode(companyCode.toUpperCase())
                .productCode(productCode)
                .parentCategoryKey(parentCategoryKey)
                .parentValue(parentValue)
                .childCategoryKey(childCategoryKey)
                .items(items)
                .servedFromCache(true)
                .lastRefreshedAt(OffsetDateTime.now())
                .build();
    }

    private Map<ReferenceCategory, List<ReferenceDataItem>> fetchAllCategoriesWithCaching(InsuranceIntegrationAdapter adapter, String companyCode, String productCode) {
        log.debug("Initiating fresh fetch for all categories from partner: {}", companyCode);
        
        Map<ReferenceCategory, List<ReferenceDataItem>> freshData = adapter.fetchMasterReferenceData(productCode);
        
        Cache cache = cacheManager.getCache(CachingConfig.MASTER_REFERENCE_DATA_CACHE);
        if (cache != null) {
            log.debug("Populating master reference data cache for {} categories", freshData.size());
            freshData.forEach((category, items) -> {
                String cacheKey = buildMasterCacheKey(companyCode, productCode, category);
                cache.put(cacheKey, items);
                log.trace("Cached category: {} with key: {} ({} items)", category, cacheKey, items.size());
            });
        } else {
            log.warn("Master reference data cache not available. Skipping population.");
        }
        
        return freshData;
    }

    @SuppressWarnings("unchecked")
    private List<ReferenceDataItem> getCategoryItemsWithCaching(InsuranceIntegrationAdapter adapter, String companyCode, String productCode, ReferenceCategory category) {
        String cacheKey = buildMasterCacheKey(companyCode, productCode, category);
        Cache cache = cacheManager.getCache(CachingConfig.MASTER_REFERENCE_DATA_CACHE);
        
        if (cache != null) {
            List<ReferenceDataItem> cachedItems = cache.get(cacheKey, List.class);
            if (cachedItems != null) {
                log.debug("Cache hit for master reference data: {}", cacheKey);
                return cachedItems;
            }
        }

        log.info("Cache miss for master reference data: {}. Fetching from partner.", cacheKey);
        Map<ReferenceCategory, List<ReferenceDataItem>> allFreshData = fetchAllCategoriesWithCaching(adapter, companyCode, productCode);
        return allFreshData.getOrDefault(category, Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    private List<ReferenceDataItem> getDependentItemsWithCaching(InsuranceIntegrationAdapter adapter, String companyCode, String productCode,
                                                                ReferenceCategory parentCategory, String parentValue,
                                                                ReferenceCategory childCategory) {
        String cacheKey = buildDependentCacheKey(companyCode, productCode, parentCategory, parentValue, childCategory);
        Cache cache = cacheManager.getCache(CachingConfig.DEPENDENT_REFERENCE_DATA_CACHE);

        if (cache != null) {
            List<ReferenceDataItem> cachedItems = cache.get(cacheKey, List.class);
            if (cachedItems != null) {
                log.debug("Cache hit for dependent reference data: {}", cacheKey);
                return cachedItems;
            }
        }

        log.info("Cache miss for dependent reference data: {}. Fetching from partner.", cacheKey);
        List<ReferenceDataItem> freshItems = adapter.fetchDependentReferenceData(productCode, parentCategory, parentValue, childCategory);
        
        if (cache != null && freshItems != null) {
            cache.put(cacheKey, freshItems);
        }

        return freshItems != null ? freshItems : Collections.emptyList();
    }

    private String buildMasterCacheKey(String companyCode, String productCode, ReferenceCategory category) {
        return String.format("%s:%s:%s", companyCode.toUpperCase(), productCode, category.name());
    }

    private String buildDependentCacheKey(String companyCode, String productCode, ReferenceCategory parentCategory, String parentValue, ReferenceCategory childCategory) {
        // Normalize parent value for cache key
        String normalizedParentValue = parentValue.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
        return String.format("%s:%s:%s:%s:%s", companyCode.toUpperCase(), productCode, parentCategory.name(), normalizedParentValue, childCategory.name());
    }

    private InsuranceIntegrationAdapter getAdapter(String companyCode) {
        return integrationAdapters.values().stream()
                .filter(adapter -> adapter.getCompanyCode().equalsIgnoreCase(companyCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported or inactive partner: " + companyCode));
    }
}
