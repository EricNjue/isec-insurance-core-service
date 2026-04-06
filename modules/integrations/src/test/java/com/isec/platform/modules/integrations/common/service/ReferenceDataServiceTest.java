package com.isec.platform.modules.integrations.common.service;

import com.isec.platform.modules.integrations.common.adapter.InsuranceIntegrationAdapter;
import com.isec.platform.modules.integrations.common.dto.referencedata.MasterReferenceDataResponse;
import com.isec.platform.modules.integrations.common.enums.ReferenceCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferenceDataServiceTest {

    @Mock
    private InsuranceIntegrationAdapter sanlamAdapter;

    @Mock
    private CacheManager cacheManager;

    private ReferenceDataService referenceDataService;

    @BeforeEach
    void setUp() {
        when(sanlamAdapter.getCompanyCode()).thenReturn("SANLAM");
        
        Map<String, InsuranceIntegrationAdapter> adapters = new HashMap<>();
        // Spring's default naming would be sanlamIntegrationAdapter
        adapters.put("sanlamIntegrationAdapter", sanlamAdapter);
        
        referenceDataService = new ReferenceDataService(adapters, cacheManager);
    }

    @Test
    void getMasterReferenceData_ShouldSucceed_WhenCompanyCodeMatches() {
        // Prepare
        String companyCode = "SANLAM";
        String productCode = "1";
        
        when(sanlamAdapter.fetchMasterReferenceData(productCode)).thenReturn(Collections.emptyMap());

        // Execute
        MasterReferenceDataResponse response = referenceDataService.getMasterReferenceData(companyCode, productCode);

        // Verify
        assertNotNull(response);
        assertEquals("SANLAM", response.getCompanyCode());
        verify(sanlamAdapter).fetchMasterReferenceData(productCode);
    }

    @Test
    void getMasterReferenceData_ShouldThrowException_WhenPartnerNotFound() {
        // Prepare
        String companyCode = "UNKNOWN";
        String productCode = "1";

        // Execute & Verify
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
                referenceDataService.getMasterReferenceData(companyCode, productCode));
        
        assertTrue(exception.getMessage().contains("Unsupported or inactive partner"));
    }

    @Test
    void getDependentReferenceData_ShouldSucceed_WhenCompanyCodeMatches() {
        // Prepare
        String companyCode = "SANLAM";
        String productCode = "1";
        ReferenceCategory parentCategory = ReferenceCategory.VEHICLE_MAKE;
        String parentValue = "B.M.W.";
        ReferenceCategory childCategory = ReferenceCategory.VEHICLE_MODEL;

        when(sanlamAdapter.fetchDependentReferenceData(productCode, parentCategory, parentValue, childCategory))
                .thenReturn(Collections.emptyList());

        // Execute
        var response = referenceDataService.getDependentReferenceData(companyCode, productCode, parentCategory, parentValue, childCategory);

        // Verify
        assertNotNull(response);
        assertEquals("SANLAM", response.getCompanyCode());
        verify(sanlamAdapter).fetchDependentReferenceData(productCode, parentCategory, parentValue, childCategory);
    }
}
