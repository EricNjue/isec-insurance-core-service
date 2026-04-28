package com.isec.platform.modules.integrations.quote.model;

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
public class QuoteBenefitsDetails {
    private Map<String, BenefitItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenefitItem {
        private BigDecimal benefit;
        private String interest;
        private String days;
        private BigDecimal extraBenefit;
        private BigDecimal clientAdditionalAmount;
    }
}
