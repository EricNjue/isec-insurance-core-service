package com.isec.platform.modules.integrations.premium.sanlam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanlamCalculatePremiumRequest {
    @JsonProperty("rate_type")
    private String rateType;
    
    @JsonProperty("vehicle_value")
    private BigDecimal vehicleValue;
    
    @JsonProperty("vehicle_make")
    private String vehicleMake;
    
    @JsonProperty("vehicle_model")
    private String vehicleModel;
    
    @JsonProperty("vehicle_year")
    private Integer vehicleYear;
    
    @JsonProperty("motor_class")
    private String motorClass;
    
    @JsonProperty("motor_subclass")
    private String motorSubclass;
    
    @JsonProperty("pvt_interest")
    private String pvtInterest;
    
    @JsonProperty("excess_protector_interest")
    private String excessProtectorInterest;
    
    @JsonProperty("windscreen_benefit")
    private BigDecimal windscreenBenefit;
    
    @JsonProperty("radio_cassette_benefit")
    private BigDecimal radioCassetteBenefit;
    
    @JsonProperty("loss_of_use_days")
    private Integer lossOfUseDays;
    
    @JsonProperty("passenger_legal_liability")
    private String passengerLegalLiability;
    
    @JsonProperty("client_identifier")
    private String clientIdentifier;
}
