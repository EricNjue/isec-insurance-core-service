package com.isec.platform.modules.rating.service;

import com.isec.platform.modules.rating.dto.RateBookDto;
import com.isec.platform.modules.rating.dto.PricingResult;
import com.isec.platform.modules.rating.domain.RuleType;
import com.isec.platform.modules.rating.dto.AddonBreakdown;
import com.isec.platform.modules.rating.dto.RatingContext;
import com.isec.platform.modules.rating.dto.ReferralDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingEngine {

    private static final BigDecimal PCF_RATE = new BigDecimal("0.0025"); // 0.25%
    private static final BigDecimal ITL_RATE = new BigDecimal("0.0020"); // 0.20%
    private static final BigDecimal CERT_CHARGE = new BigDecimal("40.00");

    private final RateBookSnapshotLoader rateBookSnapshotLoader;
    private final RuleMatcher ruleMatcher;

    public PricingResult price(RatingContext context) {
        RateBookSnapshotLoader.Snapshot snapshot = rateBookSnapshotLoader.loadActive(context.getTenantId())
                .orElseThrow(() -> new IllegalStateException("No active ratebook for tenant: " + context.getTenantId()));
        
        RateBookDto rateBook = snapshot.rateBook();
        List<RateBookDto.RateRuleDto> rules = rateBook.getRules();
        
        List<RateBookDto.RateRuleDto> sortedRules = rules.stream()
                .sorted(Comparator.comparingInt(RateBookDto.RateRuleDto::getPriority))
                .toList();

        List<Long> appliedRuleIds = new ArrayList<>();
        List<AddonBreakdown> addons = new ArrayList<>();
        ReferralDecision referralDecision = ReferralDecision.NONE;
        String referralReason = null;

        // 1. Eligibility
        boolean eligible = sortedRules.stream()
                .filter(r -> r.getRuleType() == RuleType.ELIGIBILITY && r.getCategory().equalsIgnoreCase(context.getCategory()))
                .filter(r -> ruleMatcher.matches(r, context))
                .findFirst()
                .map(r -> {
                    appliedRuleIds.add(r.getId());
                    return true;
                })
                .orElse(true); // If no eligibility rule blocks, assume eligible
        if (!eligible) {
            throw new IllegalStateException("Not eligible for cover as per rate book rules");
        }

        // 2. Referral
        var referralRuleOpt = sortedRules.stream()
                .filter(r -> r.getRuleType() == RuleType.REFERRAL && r.getCategory().equalsIgnoreCase(context.getCategory()))
                .filter(r -> ruleMatcher.matches(r, context))
                .findFirst();
        if (referralRuleOpt.isPresent()) {
            var r = referralRuleOpt.get();
            referralDecision = ReferralDecision.REFERRED;
            referralReason = r.getDescription();
            appliedRuleIds.add(r.getId());
        }

        // 3. Base premium
        BigDecimal baseRate = sortedRules.stream()
                .filter(r -> r.getRuleType() == RuleType.BASE_PREMIUM && r.getCategory().equalsIgnoreCase(context.getCategory()))
                .filter(r -> ruleMatcher.matches(r, context))
                .findFirst()
                .map(r -> {
                    appliedRuleIds.add(r.getId());
                    return ruleMatcher.evaluateBigDecimal(r, context);
                })
                .orElseThrow(() -> new IllegalStateException("No base premium rule matched for category: " + context.getCategory()));

        BigDecimal basePremium = context.getVehicleValue().multiply(baseRate).setScale(2, RoundingMode.HALF_UP);
        boolean minApplied = false;

        // 4. Minimum premium
        var minRuleOpt = sortedRules.stream()
                .filter(r -> r.getRuleType() == RuleType.MIN_PREMIUM && r.getCategory().equalsIgnoreCase(context.getCategory()))
                .filter(r -> ruleMatcher.matches(r, context))
                .findFirst();
        if (minRuleOpt.isPresent()) {
            var r = minRuleOpt.get();
            BigDecimal min = ruleMatcher.evaluateBigDecimal(r, context);
            if (min.compareTo(basePremium) > 0) {
                minApplied = true;
                basePremium = min;
            }
            appliedRuleIds.add(r.getId());
        }

        // 5. Add-ons (sum amounts)
        sortedRules.stream()
                .filter(r -> r.getRuleType() == RuleType.ADDON && r.getCategory().equalsIgnoreCase(context.getCategory()))
                .filter(r -> ruleMatcher.matches(r, context))
                .forEach(r -> {
                    BigDecimal addonAmount = ruleMatcher.evaluateBigDecimal(r, context).setScale(2, RoundingMode.HALF_UP);
                    addons.add(new AddonBreakdown(r.getDescription(), r.getDescription(), addonAmount, r.getId()));
                    appliedRuleIds.add(r.getId());
                });

        // Statutory charges
        BigDecimal pcf = basePremium.multiply(PCF_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal itl = basePremium.multiply(ITL_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal addonTotal = addons.stream().map(AddonBreakdown::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = basePremium.add(pcf).add(itl).add(CERT_CHARGE).add(addonTotal);

        return PricingResult.builder()
                .basePremium(basePremium)
                .pcf(pcf)
                .itl(itl)
                .certificateCharge(CERT_CHARGE)
                .totalPremium(total)
                .minimumPremiumApplied(minApplied)
                .referralDecision(referralDecision)
                .referralReason(referralReason)
                .appliedRuleIds(appliedRuleIds)
                .addons(addons)
                .build();
    }
}
