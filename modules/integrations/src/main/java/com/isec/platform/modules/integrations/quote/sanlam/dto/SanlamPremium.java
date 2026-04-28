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
public class SanlamPremium {
    @JsonProperty("basic_premium")
    private BigDecimal basicPremium;
    
    @JsonProperty("gross_premium")
    private BigDecimal grossPremium;
    
    @JsonProperty("sum_insured")
    private BigDecimal sumInsured;
}
