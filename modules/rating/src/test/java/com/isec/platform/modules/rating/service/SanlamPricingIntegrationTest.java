package com.isec.platform.modules.rating.service;

import com.isec.platform.modules.rating.dto.RateBookDto;
import com.isec.platform.modules.rating.dto.RatingContext;
import com.isec.platform.modules.rating.dto.PricingResult;
import com.isec.platform.modules.rating.dto.AddonBreakdown;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class SanlamPricingIntegrationTest {

    private RateBookSnapshotLoader snapshotLoader;
    private RuleMatcher ruleMatcher;
    private PricingEngine pricingEngine;

    @BeforeEach
    void setUp() {
        snapshotLoader = Mockito.mock(RateBookSnapshotLoader.class);
        ruleMatcher = new RuleMatcher(); // Use real RuleMatcher to test SpEL
        pricingEngine = new PricingEngine(snapshotLoader, ruleMatcher);
        ReflectionTestUtils.setField(pricingEngine, "pcfRate", new BigDecimal("0.0025"));
        ReflectionTestUtils.setField(pricingEngine, "itlRate", new BigDecimal("0.0020"));
        ReflectionTestUtils.setField(pricingEngine, "certCharge", new BigDecimal("40.00"));
    }

    @Test
    void price_appliesThresholdRulesCorrectly() {
        // given
        String tenantId = "SANLAM";
        RatingContext context = RatingContext.builder()
                .tenantId(tenantId)
                .category("PRIVATE_CAR")
                .vehicleValue(new BigDecimal("500000"))
                .vehicleAge(4)
                .selectedAddonIds(Set.of(101L, 102L, 103L))
                .additionalData(Map.of("courtesyCarDays", 10))
                .build();

        RateBookDto.RateRuleDto thresholdBase = RateBookDto.RateRuleDto.builder()
                .id(100L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.BASE_PREMIUM)
                .category("PRIVATE_CAR")
                .priority(4)
                .conditionExpression("vehicleValue.doubleValue() <= 600000")
                .valueExpression("37500")
                .build();

        RateBookDto.RateRuleDto defaultBase = RateBookDto.RateRuleDto.builder()
                .id(10L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.BASE_PREMIUM)
                .category("PRIVATE_CAR")
                .priority(10)
                .conditionExpression("vehicleValue.doubleValue() > 600000")
                .valueExpression("0.04")
                .build();

        RateBookDto.RateRuleDto consolidatedExcess = RateBookDto.RateRuleDto.builder()
                .id(101L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.ADDON)
                .category("PRIVATE_CAR")
                .priority(30)
                .conditionExpression("true")
                .valueExpression("vehicleValue.doubleValue() <= 600000 ? 5000.0 : T(java.lang.Math).max(5000.0, vehicleValue.doubleValue() * 0.005)")
                .description("Excess Protector")
                .build();

        RateBookDto.RateRuleDto consolidatedPvt = RateBookDto.RateRuleDto.builder()
                .id(102L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.ADDON)
                .category("PRIVATE_CAR")
                .priority(31)
                .conditionExpression("true")
                .valueExpression("vehicleValue.doubleValue() <= 600000 ? 3000.0 : T(java.lang.Math).max(3000.0, vehicleValue.doubleValue() * 0.0045)")
                .description("Political Violence & Terrorism (PVT)")
                .build();

        RateBookDto.RateRuleDto courtesyCar = RateBookDto.RateRuleDto.builder()
                .id(103L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.ADDON)
                .category("PRIVATE_CAR")
                .priority(35)
                .conditionExpression("additionalData != null && additionalData['courtesyCarDays'] != null")
                .valueExpression("(T(java.lang.Integer).parseInt(additionalData['courtesyCarDays'].toString()) / 10) * 3000")
                .description("Courtesy Car")
                .build();

        RateBookDto rb = RateBookDto.builder()
                .id(1L)
                .tenantId(tenantId)
                .rules(List.of(thresholdBase, defaultBase, consolidatedExcess, consolidatedPvt, courtesyCar))
                .build();

        when(snapshotLoader.loadActive(tenantId)).thenReturn(Mono.just(RateBookSnapshotLoader.Snapshot.from(rb)));

        // when & then
        pricingEngine.price(context)
                .as(StepVerifier::create)
                .consumeNextWith(result -> {
                    assertThat(result.getBasePremium()).isEqualByComparingTo("37500");
                    assertThat(result.getAddons()).hasSize(3);

                    AddonBreakdown excess = result.getAddons().stream().filter(a -> a.getRuleId() == 101L).findFirst().get();
                    assertThat(excess.getAmount()).isEqualByComparingTo("5000");

                    AddonBreakdown pvt = result.getAddons().stream().filter(a -> a.getRuleId() == 102L).findFirst().get();
                    assertThat(pvt.getAmount()).isEqualByComparingTo("3000");
                    assertThat(pvt.getName()).isEqualTo("Political Violence & Terrorism (PVT)");

                    AddonBreakdown courtesy = result.getAddons().stream().filter(a -> a.getRuleId() == 103L).findFirst().get();
                    assertThat(courtesy.getAmount()).isEqualByComparingTo("3000"); // 10 days / 10 * 3000 = 3000
                })
                .verifyComplete();
    }

    @Test
    void price_appliesDefaultRulesForHighValueVehicles() {
        // given
        String tenantId = "SANLAM";
        RatingContext context = RatingContext.builder()
                .tenantId(tenantId)
                .category("PRIVATE_CAR")
                .vehicleValue(new BigDecimal("1000000"))
                .vehicleAge(4)
                .selectedAddonIds(Set.of(104L))
                .build();

        RateBookDto.RateRuleDto thresholdBase = RateBookDto.RateRuleDto.builder()
                .id(100L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.BASE_PREMIUM)
                .category("PRIVATE_CAR")
                .priority(4)
                .conditionExpression("vehicleValue.doubleValue() <= 600000")
                .valueExpression("37500")
                .build();

        RateBookDto.RateRuleDto defaultBase = RateBookDto.RateRuleDto.builder()
                .id(10L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.BASE_PREMIUM)
                .category("PRIVATE_CAR")
                .priority(10)
                .conditionExpression("vehicleValue.doubleValue() > 600000")
                .valueExpression("0.04")
                .build();

        RateBookDto.RateRuleDto consolidatedExcess = RateBookDto.RateRuleDto.builder()
                .id(104L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.ADDON)
                .category("PRIVATE_CAR")
                .priority(30)
                .conditionExpression("true")
                .valueExpression("vehicleValue.doubleValue() <= 600000 ? 5000.0 : T(java.lang.Math).max(5000.0, vehicleValue.doubleValue() * 0.005)")
                .description("Excess Protector")
                .build();

        RateBookDto rb = RateBookDto.builder()
                .id(1L)
                .tenantId(tenantId)
                .rules(List.of(thresholdBase, defaultBase, consolidatedExcess))
                .build();

        when(snapshotLoader.loadActive(tenantId)).thenReturn(Mono.just(RateBookSnapshotLoader.Snapshot.from(rb)));

        // when & then
        pricingEngine.price(context)
                .as(StepVerifier::create)
                .consumeNextWith(result -> {
                    assertThat(result.getBasePremium()).isEqualByComparingTo("40000"); // 1,000,000 * 0.04
                    assertThat(result.getAddons()).hasSize(1);
                    assertThat(result.getAddons().get(0).getAmount()).isEqualByComparingTo("5000"); // max(5000, 1000000*0.005=5000)
                })
                .verifyComplete();
    }
}
