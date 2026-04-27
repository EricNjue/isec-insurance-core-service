package com.isec.platform.modules.integrations.common.adapter;

import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckRequest;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckResponse;
import com.isec.platform.modules.integrations.common.dto.referencedata.ReferenceDataItem;
import com.isec.platform.modules.integrations.common.enums.ReferenceCategory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface InsuranceIntegrationAdapter {
    String getCompanyCode();
    Mono<DoubleInsuranceCheckResponse> checkDoubleInsurance(DoubleInsuranceCheckRequest request);
    
    /**
     * Fetch all master reference data categories from the partner.
     */
    Mono<Map<ReferenceCategory, List<ReferenceDataItem>>> fetchMasterReferenceData(String productCode);

    /**
     * Fetch dependent reference data from the partner.
     */
    Mono<List<ReferenceDataItem>> fetchDependentReferenceData(String productCode, 
                                                        ReferenceCategory parentCategory, 
                                                        String parentValue, 
                                                        ReferenceCategory childCategory);
}
