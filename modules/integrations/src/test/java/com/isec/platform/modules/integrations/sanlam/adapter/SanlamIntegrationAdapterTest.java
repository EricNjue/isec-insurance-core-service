package com.isec.platform.modules.integrations.sanlam.adapter;

import com.isec.platform.common.cache.CachingConfig;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckRequest;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckResponse;
import com.isec.platform.modules.integrations.common.dto.referencedata.ReferenceDataItem;
import com.isec.platform.modules.integrations.common.enums.ReferenceCategory;
import com.isec.platform.modules.integrations.sanlam.client.SanlamClient;
import com.isec.platform.modules.integrations.sanlam.dto.SanlamDependentReferenceDataResponse;
import com.isec.platform.modules.integrations.sanlam.dto.SanlamDoubleInsuranceResponse;
import com.isec.platform.modules.integrations.sanlam.dto.SanlamMasterReferenceDataResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SanlamIntegrationAdapterTest {

    @Mock
    private SanlamClient sanlamClient;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private SanlamIntegrationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SanlamIntegrationAdapter(sanlamClient, cacheManager);
    }

    @Test
    void getCompanyCode_ShouldReturnSanlam() {
        assertEquals("SANLAM", adapter.getCompanyCode());
    }

    @Test
    void checkDoubleInsurance_ShouldReturnCanonicalResponse_WhenCacheMiss() {
        // Prepare
        DoubleInsuranceCheckRequest request = DoubleInsuranceCheckRequest.builder()
                .registrationNumber("KAA123X")
                .chassisNumber("CH123")
                .build();
        String cacheKey = "KAA123X-CH123";

        SanlamDoubleInsuranceResponse sanlamResponse = new SanlamDoubleInsuranceResponse();
        sanlamResponse.setStatus("clear");
        sanlamResponse.setMessage("No active cover");

        when(cacheManager.getCache(CachingConfig.SANLAM_DOUBLE_INSURANCE_CACHE)).thenReturn(cache);
        when(cache.get(cacheKey, DoubleInsuranceCheckResponse.class)).thenReturn(null);
        when(sanlamClient.checkDoubleInsurance(anyString(), anyString())).thenReturn(Mono.just(sanlamResponse));

        // Execute & Verify
        StepVerifier.create(adapter.checkDoubleInsurance(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.isHasDuplicate());
                    assertEquals("clear", response.getStatus());
                })
                .verifyComplete();

        verify(sanlamClient).checkDoubleInsurance("KAA123X", "CH123");
        verify(cache).put(eq(cacheKey), any(DoubleInsuranceCheckResponse.class));
    }

    @Test
    void checkDoubleInsurance_ShouldReturnCachedResponse_WhenCacheHit() {
        // Prepare
        DoubleInsuranceCheckRequest request = DoubleInsuranceCheckRequest.builder()
                .registrationNumber("KAA123X")
                .chassisNumber("CH123")
                .build();
        String cacheKey = "KAA123X-CH123";

        DoubleInsuranceCheckResponse cachedResponse = DoubleInsuranceCheckResponse.builder()
                .hasDuplicate(false)
                .status("clear")
                .build();

        when(cacheManager.getCache(CachingConfig.SANLAM_DOUBLE_INSURANCE_CACHE)).thenReturn(cache);
        when(cache.get(cacheKey, DoubleInsuranceCheckResponse.class)).thenReturn(cachedResponse);

        // Execute & Verify
        StepVerifier.create(adapter.checkDoubleInsurance(request))
                .expectNext(cachedResponse)
                .verifyComplete();

        verifyNoInteractions(sanlamClient);
    }

    @Test
    void checkDoubleInsurance_WithDuplicate_ShouldReturnDetails() {
        // Prepare
        DoubleInsuranceCheckRequest request = DoubleInsuranceCheckRequest.builder()
                .registrationNumber("KAA123X")
                .chassisNumber("CH123")
                .build();

        SanlamDoubleInsuranceResponse sanlamResponse = new SanlamDoubleInsuranceResponse();
        sanlamResponse.setStatus("double");
        sanlamResponse.setMessage("Active cover found");
        
        SanlamDoubleInsuranceResponse.Evidence evidence = new SanlamDoubleInsuranceResponse.Evidence();
        evidence.setInsurer("APA");
        evidence.setPolicyNumber("POL123");
        evidence.setCoverEndDate("2026-12-31");
        sanlamResponse.setEvidence(evidence);

        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(sanlamClient.checkDoubleInsurance(anyString(), anyString())).thenReturn(Mono.just(sanlamResponse));

        // Execute & Verify
        StepVerifier.create(adapter.checkDoubleInsurance(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.isHasDuplicate());
                    assertNotNull(response.getDetails());
                    assertEquals("APA", response.getDetails().getInsurer());
                    assertEquals("POL123", response.getDetails().getPolicyNumber());
                })
                .verifyComplete();
    }
    
    @Test
    void checkDoubleInsurance_WithNullChassis_ShouldCallClientWithNull() {
        // Prepare
        DoubleInsuranceCheckRequest request = DoubleInsuranceCheckRequest.builder()
                .registrationNumber("KAA123X")
                .chassisNumber(null)
                .build();

        SanlamDoubleInsuranceResponse sanlamResponse = new SanlamDoubleInsuranceResponse();
        sanlamResponse.setStatus("clear");

        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(sanlamClient.checkDoubleInsurance(anyString(), eq(null))).thenReturn(Mono.just(sanlamResponse));

        // Execute & Verify
        StepVerifier.create(adapter.checkDoubleInsurance(request))
                .expectNextCount(1)
                .verifyComplete();

        verify(sanlamClient).checkDoubleInsurance("KAA123X", null);
    }

    @Test
    void fetchMasterReferenceData_ShouldReturnMappedCategories() {
        // Prepare
        String productCode = "1";
        SanlamMasterReferenceDataResponse.SanlamReferenceDataItem item = new SanlamMasterReferenceDataResponse.SanlamReferenceDataItem();
        item.setValue("Motor Commercial");
        item.setValueCode("1001");
        item.setStrAttrSysId("5");

        Map<String, List<SanlamMasterReferenceDataResponse.SanlamReferenceDataItem>> categoryMap = Map.of(
                "Policy Type", List.of(item)
        );
        SanlamMasterReferenceDataResponse sanlamResponse = new SanlamMasterReferenceDataResponse(Map.of("1", categoryMap));

        when(sanlamClient.fetchMasterReferenceData(productCode)).thenReturn(Mono.just(sanlamResponse));

        // Execute & Verify
        StepVerifier.create(adapter.fetchMasterReferenceData(productCode))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertTrue(result.containsKey(ReferenceCategory.POLICY_TYPE));
                    List<ReferenceDataItem> items = result.get(ReferenceCategory.POLICY_TYPE);
                    assertEquals(1, items.size());
                    assertEquals("1001", items.get(0).getCode());
                    assertEquals("Motor Commercial", items.get(0).getLabel());
                    assertEquals("5", items.get(0).getSourceId());
                })
                .verifyComplete();
    }

    @Test
    void fetchDependentReferenceData_ShouldReturnMappedItems() {
        // Prepare
        String productCode = "1";
        SanlamMasterReferenceDataResponse.SanlamReferenceDataItem item = new SanlamMasterReferenceDataResponse.SanlamReferenceDataItem();
        item.setValue("X5");
        item.setValueCode("X002");
        item.setStrAttrSysId("272");

        SanlamDependentReferenceDataResponse sanlamResponse = new SanlamDependentReferenceDataResponse(Map.of(
                "Vehicle Model", List.of(item)
        ));

        when(sanlamClient.fetchDependentReferenceData("Vehicle Make", "B.M.W.", "Vehicle Model")).thenReturn(Mono.just(sanlamResponse));

        // Execute & Verify
        StepVerifier.create(adapter.fetchDependentReferenceData(productCode, 
                ReferenceCategory.VEHICLE_MAKE, "B.M.W.", ReferenceCategory.VEHICLE_MODEL))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(1, result.size());
                    assertEquals("X002", result.get(0).getCode());
                    assertEquals("X5", result.get(0).getLabel());
                    assertEquals("272", result.get(0).getSourceId());
                })
                .verifyComplete();
    }
}
