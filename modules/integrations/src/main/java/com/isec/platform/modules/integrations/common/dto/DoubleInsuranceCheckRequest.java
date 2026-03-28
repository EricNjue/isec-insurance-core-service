package com.isec.platform.modules.integrations.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoubleInsuranceCheckRequest {
    private String registrationNumber;
    private String chassisNumber;
}
