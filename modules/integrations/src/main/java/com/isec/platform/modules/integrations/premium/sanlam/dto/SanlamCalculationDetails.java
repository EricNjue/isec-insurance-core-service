package com.isec.platform.modules.integrations.premium.sanlam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanlamCalculationDetails {
    private Long baseRateSetId;
    private String baseRateSetName;
    private boolean specialRateApplied;
    private boolean pvtInclusiveApplicable;
    private boolean excessProtectorInclusiveApplicable;
}
