package com.isec.platform.modules.integrations.premium.model;

import com.isec.platform.modules.integrations.premium.provider.PremiumProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumCalculationRequest {
    private PremiumProviderType provider;
    private String rateType;
    private BigDecimal vehicleValue;
    private String vehicleMake;
    private String vehicleModel;
    private Integer vehicleYear;
    private String motorClass;
    private String motorSubclass;
    private String pvtInterest;
    private String excessProtectorInterest;
    private BigDecimal windscreenBenefit;
    private BigDecimal radioCassetteBenefit;
    private Integer lossOfUseDays;
    private String passengerLegalLiability;
    private String clientIdentifier;
    private Map<String, Object> metadata;
}
