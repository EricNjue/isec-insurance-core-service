package com.isec.platform.modules.rating.service;

import com.isec.platform.modules.rating.dto.RateBookDto;
import com.isec.platform.modules.rating.dto.RatingContext;
import com.isec.platform.modules.rating.dto.PricingResult;
import com.isec.platform.modules.rating.dto.AddonBreakdown;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ThresholdPricingTest {

    private RateBookSnapshotLoader snapshotLoader;
    private RuleMatcher ruleMatcher;
    private PricingEngine pricingEngine;

    @BeforeEach
    void setUp() {
        snapshotLoader = Mockito.mock(RateBookSnapshotLoader.class);
        ruleMatcher = Mockito.mock(RuleMatcher.class);
        pricingEngine = new PricingEngine(snapshotLoader, ruleMatcher);
        ReflectionTestUtils.setField(pricingEngine, "pcfRate", new BigDecimal("0.0025"));
        ReflectionTestUtils.setField(pricingEngine, "itlRate", new BigDecimal("0.0020"));
        ReflectionTestUtils.setField(pricingEngine, "certCharge", new BigDecimal("40.00"));
    }

    @Test
    void price_appliesFlatBasePremiumForLowValueVehicles() {
        // given
        String tenantId = "SANLAM";
        RatingContext context = RatingContext.builder()
                .tenantId(tenantId)
                .category("PRIVATE_CAR")
                .vehicleValue(new BigDecimal("600000"))
                .build();

        RateBookDto.RateRuleDto thresholdRule = RateBookDto.RateRuleDto.builder()
                .id(100L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.BASE_PREMIUM)
                .category("PRIVATE_CAR")
                .description("Basic premium for vehicles <= 600k")
                .priority(5)
                .conditionExpression("vehicleValue <= 600000")
                .build();

        RateBookDto.RateRuleDto defaultRule = RateBookDto.RateRuleDto.builder()
                .id(10L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.BASE_PREMIUM)
                .category("PRIVATE_CAR")
                .description("Sanlam Private Car Base Rate")
                .priority(10)
                .build();

        RateBookDto rb = RateBookDto.builder()
                .id(1L)
                .tenantId(tenantId)
                .rules(List.of(thresholdRule, defaultRule))
                .build();

        when(snapshotLoader.loadActive(tenantId)).thenReturn(RateBookSnapshotLoader.Snapshot.from(rb));
        when(ruleMatcher.matches(eq(thresholdRule), any())).thenReturn(true);
        when(ruleMatcher.matches(eq(defaultRule), any())).thenReturn(true);
        when(ruleMatcher.evaluateBigDecimal(eq(thresholdRule), any())).thenReturn(new BigDecimal("37500"));
        when(ruleMatcher.evaluateBigDecimal(eq(defaultRule), any())).thenReturn(new BigDecimal("0.04"));

        // when
        PricingResult result = pricingEngine.price(context);

        // then
        assertThat(result.getBasePremium()).isEqualByComparingTo("37500");
    }

    @Test
    void price_appliesFlatBasePremiumForLowValueVehicles_Commercial() {
        // given
        String tenantId = "SANLAM";
        RatingContext context = RatingContext.builder()
                .tenantId(tenantId)
                .category("COMMERCIAL")
                .vehicleValue(new BigDecimal("600000"))
                .build();

        RateBookDto.RateRuleDto thresholdRuleCommercial = RateBookDto.RateRuleDto.builder()
                .id(100L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.BASE_PREMIUM)
                .category("COMMERCIAL")
                .description("Basic premium for vehicles <= 600k (Commercial)")
                .priority(5)
                .conditionExpression("vehicleValue <= 600000")
                .build();

        RateBookDto.RateRuleDto defaultRuleCommercial = RateBookDto.RateRuleDto.builder()
                .id(11L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.BASE_PREMIUM)
                .category("COMMERCIAL")
                .description("Sanlam Commercial Base Rate")
                .priority(10)
                .build();

        RateBookDto rb = RateBookDto.builder()
                .id(1L)
                .tenantId(tenantId)
                .rules(List.of(thresholdRuleCommercial, defaultRuleCommercial))
                .build();

        when(snapshotLoader.loadActive(tenantId)).thenReturn(RateBookSnapshotLoader.Snapshot.from(rb));
        when(ruleMatcher.matches(eq(thresholdRuleCommercial), any())).thenReturn(true);
        when(ruleMatcher.matches(eq(defaultRuleCommercial), any())).thenReturn(true);
        when(ruleMatcher.evaluateBigDecimal(eq(thresholdRuleCommercial), any())).thenReturn(new BigDecimal("37500"));
        when(ruleMatcher.evaluateBigDecimal(eq(defaultRuleCommercial), any())).thenReturn(new BigDecimal("0.06"));

        // when
        PricingResult result = pricingEngine.price(context);

        // then
        assertThat(result.getBasePremium()).isEqualByComparingTo("37500");
    }

    @Test
    void price_appliesAddonsForLowValueVehicles() {
        // given
        String tenantId = "SANLAM";
        RatingContext context = RatingContext.builder()
                .tenantId(tenantId)
                .category("PRIVATE_CAR")
                .vehicleValue(new BigDecimal("500000"))
                .selectedAddonIds(Set.of(101L, 102L, 103L))
                .additionalData(Map.of("courtesyCarDays", 20))
                .build();

        RateBookDto.RateRuleDto baseRule = RateBookDto.RateRuleDto.builder()
                .id(100L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.BASE_PREMIUM)
                .category("PRIVATE_CAR")
                .priority(5)
                .build();

        RateBookDto.RateRuleDto excessRule = RateBookDto.RateRuleDto.builder()
                .id(101L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.ADDON)
                .category("PRIVATE_CAR")
                .description("Excess Protector")
                .priority(25)
                .build();

        RateBookDto.RateRuleDto pvtRule = RateBookDto.RateRuleDto.builder()
                .id(102L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.ADDON)
                .category("PRIVATE_CAR")
                .description("PVT")
                .priority(26)
                .build();

        RateBookDto.RateRuleDto courtesyRule = RateBookDto.RateRuleDto.builder()
                .id(103L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.ADDON)
                .category("PRIVATE_CAR")
                .description("Courtesy Car")
                .priority(27)
                .build();

        RateBookDto rb = RateBookDto.builder()
                .id(1L)
                .tenantId(tenantId)
                .rules(List.of(baseRule, excessRule, pvtRule, courtesyRule))
                .build();

        when(snapshotLoader.loadActive(tenantId)).thenReturn(RateBookSnapshotLoader.Snapshot.from(rb));
        when(ruleMatcher.matches(any(), any())).thenReturn(true);
        when(ruleMatcher.evaluateBigDecimal(eq(baseRule), any())).thenReturn(new BigDecimal("37500"));
        when(ruleMatcher.evaluateBigDecimal(eq(excessRule), any())).thenReturn(new BigDecimal("5000"));
        when(ruleMatcher.evaluateBigDecimal(eq(pvtRule), any())).thenReturn(new BigDecimal("3000"));
        // Simulating the SpEL evaluation for courtesy car: (20 / 10) * 3000 = 6000
        when(ruleMatcher.evaluateBigDecimal(eq(courtesyRule), any())).thenReturn(new BigDecimal("6000"));

        // when
        PricingResult result = pricingEngine.price(context);

        // then
        assertThat(result.getBasePremium()).isEqualByComparingTo("37500");
        assertThat(result.getAddons()).hasSize(3);
        
        BigDecimal addonTotal = result.getAddons().stream()
                .map(AddonBreakdown::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertThat(addonTotal).isEqualByComparingTo("14000"); // 5000 + 3000 + 6000
    }
}
