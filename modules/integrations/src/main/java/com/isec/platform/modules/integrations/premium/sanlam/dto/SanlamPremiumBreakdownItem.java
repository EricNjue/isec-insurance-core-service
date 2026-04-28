package com.isec.platform.modules.integrations.premium.sanlam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanlamPremiumBreakdownItem {
    private String label;
    private BigDecimal amount;
}
