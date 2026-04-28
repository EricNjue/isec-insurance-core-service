package com.isec.platform.modules.integrations.premium.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumCalculationMetadata {
    private Long baseRateSetId;
    private String baseRateSetName;
    private boolean specialRateApplied;
    private boolean pvtInclusiveApplicable;
    private boolean excessProtectorInclusiveApplicable;
    private Map<String, Object> additionalAttributes;
}
