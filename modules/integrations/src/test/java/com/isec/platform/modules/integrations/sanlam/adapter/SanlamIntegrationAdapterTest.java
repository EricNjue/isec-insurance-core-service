package com.isec.platform.modules.integrations.sanlam.adapter;

import com.isec.platform.common.cache.CachingConfig;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckRequest;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckResponse;
import com.isec.platform.modules.integrations.sanlam.client.SanlamClient;
import com.isec.platform.modules.integrations.sanlam.dto.SanlamDoubleInsuranceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

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
        when(sanlamClient.checkDoubleInsurance(anyString(), anyString())).thenReturn(sanlamResponse);

        // Execute
        DoubleInsuranceCheckResponse response = adapter.checkDoubleInsurance(request);

        // Verify
        assertNotNull(response);
        assertFalse(response.isHasDuplicate());
        assertEquals("clear", response.getStatus());
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

        // Execute
        DoubleInsuranceCheckResponse response = adapter.checkDoubleInsurance(request);

        // Verify
        assertNotNull(response);
        assertEquals(cachedResponse, response);
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
        when(sanlamClient.checkDoubleInsurance(anyString(), anyString())).thenReturn(sanlamResponse);

        // Execute
        DoubleInsuranceCheckResponse response = adapter.checkDoubleInsurance(request);

        // Verify
        assertNotNull(response);
        assertTrue(response.isHasDuplicate());
        assertNotNull(response.getDetails());
        assertEquals("APA", response.getDetails().getInsurer());
        assertEquals("POL123", response.getDetails().getPolicyNumber());
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
        when(sanlamClient.checkDoubleInsurance(anyString(), eq(null))).thenReturn(sanlamResponse);

        // Execute
        adapter.checkDoubleInsurance(request);

        // Verify
        verify(sanlamClient).checkDoubleInsurance("KAA123X", null);
    }
}
