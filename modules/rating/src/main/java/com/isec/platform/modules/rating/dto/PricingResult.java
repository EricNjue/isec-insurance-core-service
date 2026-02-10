package com.isec.platform.modules.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingResult {
    private BigDecimal basePremium;
    private BigDecimal pcf; // policyholder compensation fund
    private BigDecimal itl; // training levy
    private BigDecimal certificateCharge;
    private BigDecimal totalPremium;

    private boolean minimumPremiumApplied;
    private ReferralDecision referralDecision;
    private String referralReason;

    @Builder.Default
    private List<Long> appliedRuleIds = new ArrayList<>();

    @Builder.Default
    private List<AddonBreakdown> addons = new ArrayList<>();

    public void addAppliedRule(Long id) {
        if (id != null) this.appliedRuleIds.add(id);
    }
}
