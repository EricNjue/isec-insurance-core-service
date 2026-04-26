package com.isec.platform.modules.rating.service;

import com.isec.platform.modules.rating.dto.RateBookDto;
import com.isec.platform.modules.rating.dto.PricingResult;
import com.isec.platform.modules.rating.domain.RuleType;
import com.isec.platform.modules.rating.dto.AddonBreakdown;
import com.isec.platform.modules.rating.dto.RatingContext;
import com.isec.platform.modules.rating.dto.ReferralDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingEngine {

    @Value("${rating.charges.pcf-rate:0.0025}")
    private BigDecimal pcfRate;

    @Value("${rating.charges.itl-rate:0.0020}")
    private BigDecimal itlRate;

    @Value("${rating.charges.cert-charge:40.00}")
    private BigDecimal certCharge;

    private final RateBookSnapshotLoader rateBookSnapshotLoader;
    private final RuleMatcher ruleMatcher;

    public Mono<PricingResult> price(RatingContext context) {
        return rateBookSnapshotLoader.loadActive(context.getTenantId())
                .switchIfEmpty(Mono.error(new IllegalStateException("No active ratebook for tenant: " + context.getTenantId())))
                .map(snapshot -> {
                    RateBookDto rateBook = snapshot.rateBook();
                    List<RateBookDto.RateRuleDto> sortedRules = rateBook.getRules().stream()
                            .sorted(Comparator.comparingInt(RateBookDto.RateRuleDto::getPriority))
                            .toList();

                    List<Long> appliedRuleIds = new ArrayList<>();

                    // 1. Eligibility
                    checkEligibility(context, sortedRules, appliedRuleIds);

                    // 2. Referral
                    ReferralInfo referralInfo = checkReferral(context, sortedRules, appliedRuleIds);

                    // 3. Base premium
                    BigDecimal basePremium = calculateBasePremium(context, sortedRules, appliedRuleIds);

                    // 4. Minimum premium
                    boolean minApplied = false;
                    BigDecimal adjustedBasePremium = applyMinimumPremium(context, sortedRules, appliedRuleIds, basePremium);
                    if (adjustedBasePremium.compareTo(basePremium) > 0) {
                        minApplied = true;
                        basePremium = adjustedBasePremium;
                    }

                    // 5. Add-ons
                    List<AddonBreakdown> addons = calculateAddons(context, sortedRules, appliedRuleIds);

                    // 6. Statutory charges & Total
                    return buildPricingResult(basePremium, addons, referralInfo, minApplied, appliedRuleIds);
                });
    }

    private void checkEligibility(RatingContext context, List<RateBookDto.RateRuleDto> rules, List<Long> appliedRuleIds) {
        rules.stream()
                .filter(r -> r.getRuleType() == RuleType.ELIGIBILITY && r.getCategory().equalsIgnoreCase(context.getCategory()))
                .forEach(r -> {
                    boolean matches = ruleMatcher.matches(r, context);
                    log.debug("Evaluating eligibility rule {}: {}, result: {}", r.getId(), r.getDescription(), matches);
                    if (!matches) {
                        throw new IllegalStateException("Not eligible for cover: " + r.getDescription());
                    }
                    appliedRuleIds.add(r.getId());
                });
    }

    private ReferralInfo checkReferral(RatingContext context, List<RateBookDto.RateRuleDto> rules, List<Long> appliedRuleIds) {
        return rules.stream()
                .filter(r -> r.getRuleType() == RuleType.REFERRAL && r.getCategory().equalsIgnoreCase(context.getCategory()))
                .filter(r -> ruleMatcher.matches(r, context))
                .findFirst()
                .map(r -> {
                    appliedRuleIds.add(r.getId());
                    return new ReferralInfo(ReferralDecision.REFERRED, r.getDescription());
                })
                .orElse(new ReferralInfo(ReferralDecision.NONE, null));
    }

    private BigDecimal calculateBasePremium(RatingContext context, List<RateBookDto.RateRuleDto> rules, List<Long> appliedRuleIds) {
        return rules.stream()
                .filter(r -> r.getRuleType() == RuleType.BASE_PREMIUM && r.getCategory().equalsIgnoreCase(context.getCategory()))
                .filter(r -> ruleMatcher.matches(r, context))
                .findFirst()
                .map(r -> {
                    appliedRuleIds.add(r.getId());
                    BigDecimal value = ruleMatcher.evaluateBigDecimal(r, context);
                    // If the value is > 1, treat it as a flat premium, otherwise treat as a rate
                    if (value.compareTo(BigDecimal.ONE) > 0) {
                        return value.setScale(0, RoundingMode.UP);
                    }
                    return context.getVehicleValue().multiply(value).setScale(0, RoundingMode.UP);
                })
                .orElseThrow(() -> new IllegalStateException("No base premium rule matched for category: " + context.getCategory()));
    }

    private BigDecimal applyMinimumPremium(RatingContext context, List<RateBookDto.RateRuleDto> rules, List<Long> appliedRuleIds, BigDecimal basePremium) {
        return rules.stream()
                .filter(r -> r.getRuleType() == RuleType.MIN_PREMIUM && r.getCategory().equalsIgnoreCase(context.getCategory()))
                .filter(r -> ruleMatcher.matches(r, context))
                .findFirst()
                .map(r -> {
                    appliedRuleIds.add(r.getId());
                    BigDecimal min = ruleMatcher.evaluateBigDecimal(r, context);
                    return min.max(basePremium);
                })
                .orElse(basePremium);
    }

    private List<AddonBreakdown> calculateAddons(RatingContext context, List<RateBookDto.RateRuleDto> rules, List<Long> appliedRuleIds) {
        List<AddonBreakdown> addons = new ArrayList<>();
        rules.stream()
                .filter(r -> r.getRuleType() == RuleType.ADDON && r.getCategory().equalsIgnoreCase(context.getCategory()))
                .filter(r -> context.getSelectedAddonIds() != null && context.getSelectedAddonIds().contains(r.getId()))
                .filter(r -> ruleMatcher.matches(r, context))
                .forEach(r -> {
                    BigDecimal addonAmount = ruleMatcher.evaluateBigDecimal(r, context).setScale(0, RoundingMode.UP);
                    addons.add(new AddonBreakdown(r.getDescription(), r.getDescription(), addonAmount, r.getId()));
                    appliedRuleIds.add(r.getId());
                });
        return addons;
    }

    private PricingResult buildPricingResult(BigDecimal basePremium, List<AddonBreakdown> addons, ReferralInfo referralInfo, boolean minApplied, List<Long> appliedRuleIds) {
        BigDecimal pcf = basePremium.multiply(pcfRate).setScale(0, RoundingMode.UP);
        BigDecimal itl = basePremium.multiply(itlRate).setScale(0, RoundingMode.UP);
        BigDecimal addonTotal = addons.stream().map(AddonBreakdown::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = basePremium.add(pcf).add(itl).add(certCharge).add(addonTotal).setScale(0, RoundingMode.UP);

        return PricingResult.builder()
                .basePremium(basePremium)
                .pcf(pcf)
                .itl(itl)
                .certificateCharge(certCharge)
                .totalPremium(total)
                .minimumPremiumApplied(minApplied)
                .referralDecision(referralInfo.decision())
                .referralReason(referralInfo.reason())
                .appliedRuleIds(appliedRuleIds)
                .addons(addons)
                .build();
    }

    private record ReferralInfo(ReferralDecision decision, String reason) {}
}
