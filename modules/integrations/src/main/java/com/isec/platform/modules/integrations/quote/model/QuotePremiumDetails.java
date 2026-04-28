package com.isec.platform.modules.integrations.quote.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotePremiumDetails {
    private BigDecimal basicPremium;
    private BigDecimal grossPremium;
    private BigDecimal netPremium;
    private BigDecimal levies;
    private BigDecimal stampDuty;
    private BigDecimal sumInsured;
}
