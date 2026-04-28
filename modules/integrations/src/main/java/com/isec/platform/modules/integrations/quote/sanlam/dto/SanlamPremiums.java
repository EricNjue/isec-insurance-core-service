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
public class SanlamPremiums {
    @JsonProperty("basic")
    private BigDecimal basic;
    
    @JsonProperty("gross")
    private BigDecimal gross;
    
    @JsonProperty("net")
    private BigDecimal net;
    
    @JsonProperty("levies")
    private BigDecimal levies;
    
    @JsonProperty("stamp_duty")
    private BigDecimal stampDuty;
}
