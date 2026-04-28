package com.isec.platform.modules.integrations.quote.sanlam.dto;

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
public class SanlamBenefits {
    @JsonProperty("pvt")
    private SanlamBenefit pvt;
    
    @JsonProperty("excess_protector")
    private SanlamBenefit excessProtector;
    
    @JsonProperty("courtesy_car")
    private SanlamBenefit courtesyCar;
    
    @JsonProperty("windscreen")
    private SanlamWindscreenBenefit windscreen;
    
    @JsonProperty("radio_cassette")
    private SanlamWindscreenBenefit radioCassette;
    
    @JsonProperty("passenger_legal_liability")
    private SanlamBenefit passengerLegalLiability;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanlamBenefit {
        private BigDecimal benefit;
        private String interest;
        private String days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanlamWindscreenBenefit {
        private BigDecimal benefit;
        @JsonProperty("extra_benefit")
        private BigDecimal extraBenefit;
        @JsonProperty("client_additional_amount")
        private BigDecimal clientAdditionalAmount;
    }
}
