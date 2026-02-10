package com.isec.platform.modules.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddonBreakdown {
    private String code;
    private String name;
    private BigDecimal amount;
    private Long ruleId;
}
