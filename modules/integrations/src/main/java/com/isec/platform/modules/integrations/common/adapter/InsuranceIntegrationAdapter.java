package com.isec.platform.modules.integrations.common.adapter;

import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckRequest;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckResponse;

public interface InsuranceIntegrationAdapter {
    String getCompanyCode();
    DoubleInsuranceCheckResponse checkDoubleInsurance(DoubleInsuranceCheckRequest request);
}
