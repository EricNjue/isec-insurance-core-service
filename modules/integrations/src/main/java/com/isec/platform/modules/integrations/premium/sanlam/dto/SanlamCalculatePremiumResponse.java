package com.isec.platform.modules.integrations.premium.sanlam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class SanlamCalculatePremiumResponse {
    @JsonProperty("basic_premium")
    private BigDecimal basicPremium;
    
    @JsonProperty("pvt_benefit")
    private BigDecimal pvtBenefit;
    
    @JsonProperty("excess_protector_benefit")
    private BigDecimal excessProtectorBenefit;
    
    @JsonProperty("windscreen_benefit")
    private BigDecimal windscreenBenefit;
    
    @JsonProperty("radio_cassette_benefit")
    private BigDecimal radioCassetteBenefit;
    
    @JsonProperty("loss_of_use_benefit")
    private BigDecimal lossOfUseBenefit;
    
    @JsonProperty("passenger_legal_liability_benefit")
    private BigDecimal passengerLegalLiabilityBenefit;
    
    @JsonProperty("benefits_total")
    private BigDecimal benefitsTotal;
    
    @JsonProperty("benefits_breakdown")
    private List<SanlamPremiumBreakdownItem> benefitsBreakdown;
    
    @JsonProperty("gross_premium_breakdown")
    private List<SanlamPremiumBreakdownItem> grossPremiumBreakdown;
    
    @JsonProperty("net_premium")
    private BigDecimal netPremium;
    
    @JsonProperty("levies")
    private BigDecimal levies;
    
    @JsonProperty("stamp_duty")
    private BigDecimal stampDuty;
    
    @JsonProperty("gross_premium")
    private BigDecimal grossPremium;
    
    @JsonProperty("rate_set_used")
    private String rateSetUsed;
    
    @JsonProperty("special_rate_applied")
    private boolean specialRateApplied;
    
    @JsonProperty("calculation_details")
    private SanlamCalculationDetails calculationDetails;
    
    @JsonProperty("prod_sys_id")
    private Integer productSystemId;
    
    @JsonProperty("prod_code")
    private String productCode;
}
