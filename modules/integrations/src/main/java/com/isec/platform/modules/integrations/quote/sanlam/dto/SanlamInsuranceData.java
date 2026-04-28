package com.isec.platform.modules.integrations.quote.sanlam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanlamInsuranceData {
    @JsonProperty("rate_engine")
    private SanlamRateEngine rateEngine;
    
    @JsonProperty("vehicle")
    private SanlamVehicle vehicle;
    
    @JsonProperty("premium")
    private SanlamPremium premium;
    
    @JsonProperty("premiums")
    private SanlamPremiums premiums;
    
    @JsonProperty("benefits")
    private SanlamBenefits benefits;
    
    @JsonProperty("subclass")
    private String subclass;
    
    @JsonProperty("vehicle_type")
    private String vehicleType;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("client")
    private SanlamClient client;
    
    @JsonProperty("cover")
    private SanlamCover cover;
    
    @JsonProperty("disclaimers")
    private SanlamDisclaimers disclaimers;
    
    @JsonProperty("dmvic_check")
    private SanlamDmvicCheck dmvicCheck;
    
    @JsonProperty("submitted_at")
    private LocalDateTime submittedAt;
}
