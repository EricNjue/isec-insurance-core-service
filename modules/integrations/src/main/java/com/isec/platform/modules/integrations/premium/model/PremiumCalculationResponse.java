package com.isec.platform.modules.integrations.premium.model;

import com.isec.platform.modules.integrations.premium.provider.PremiumProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumCalculationResponse {
    private PremiumProviderType provider;
    private PremiumCalculationStatus status;
    private BigDecimal basicPremium;
    private BigDecimal pvtBenefit;
    private BigDecimal excessProtectorBenefit;
    private BigDecimal windscreenBenefit;
    private BigDecimal radioCassetteBenefit;
    private BigDecimal lossOfUseBenefit;
    private BigDecimal passengerLegalLiabilityBenefit;
    private BigDecimal benefitsTotal;
    private List<PremiumBenefitBreakdown> benefitsBreakdown;
    private List<PremiumGrossBreakdown> grossPremiumBreakdown;
    private BigDecimal netPremium;
    private BigDecimal levies;
    private BigDecimal stampDuty;
    private BigDecimal grossPremium;
    private String rateSetUsed;
    private boolean specialRateApplied;
    private Integer productSystemId;
    private String productCode;
    private PremiumCalculationMetadata calculationMetadata;
    private String rawResponse;
}
