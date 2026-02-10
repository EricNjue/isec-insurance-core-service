package com.isec.platform.modules.rating.service;

import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.rating.dto.RateBookDto;
import com.isec.platform.modules.rating.dto.RatingContext;
import com.isec.platform.modules.rating.dto.PricingResult;
import com.isec.platform.modules.rating.dto.ReferralDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PricingEngineTest {

    private RateBookSnapshotLoader snapshotLoader;
    private RuleMatcher ruleMatcher;
    private PricingEngine pricingEngine;

    @BeforeEach
    void setUp() {
        snapshotLoader = Mockito.mock(RateBookSnapshotLoader.class);
        ruleMatcher = Mockito.mock(RuleMatcher.class);
        pricingEngine = new PricingEngine(snapshotLoader, ruleMatcher);
    }

    @Test
    void price_roundsUpCalculations() {
        // given
        String tenantId = "TENANT1";
        RatingContext context = RatingContext.builder()
                .tenantId(tenantId)
                .category("PRIVATE_CAR")
                .vehicleValue(new BigDecimal("1000000"))
                .build();

        RateBookDto.RateRuleDto baseRule = RateBookDto.RateRuleDto.builder()
                .id(1L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.BASE_PREMIUM)
                .category("PRIVATE_CAR")
                .priority(10)
                .build();

        RateBookDto rb = RateBookDto.builder()
                .id(1L)
                .tenantId(tenantId)
                .versionName("v1.0")
                .rules(List.of(baseRule))
                .build();

        when(snapshotLoader.loadActive(tenantId)).thenReturn(RateBookSnapshotLoader.Snapshot.from(rb));
        when(ruleMatcher.matches(any(), any())).thenReturn(true);
        // 1,000,000 * 0.045678 = 45,600.00 exactly if 0.0456? 
        // Let's use a value that definitely has decimals: 1,000,000 * 0.04567 = 45,670
        // Or 1,000,005 * 0.04 = 40,000.2 -> shd be 40,001
        context.setVehicleValue(new BigDecimal("1000005"));
        when(ruleMatcher.evaluateBigDecimal(eq(baseRule), any())).thenReturn(new BigDecimal("0.04"));

        // when
        PricingResult result = pricingEngine.price(context);

        // then
        // base = 1000005 * 0.04 = 40000.2 -> 40001
        assertThat(result.getBasePremium()).isEqualByComparingTo("40001");
        
        // pcf = 40001 * 0.0025 = 100.0025 -> 101
        assertThat(result.getPcf()).isEqualByComparingTo("101");
        
        // itl = 40001 * 0.0020 = 80.002 -> 81
        assertThat(result.getItl()).isEqualByComparingTo("81");
        
        // total = 40001 + 101 + 81 + 35 = 40218
        assertThat(result.getTotalPremium()).isEqualByComparingTo("40218");
    }

    @Test
    void price_appliesMinimumPremium() {
        // given
        String tenantId = "TENANT1";
        RatingContext context = RatingContext.builder()
                .tenantId(tenantId)
                .category("PRIVATE_CAR")
                .vehicleValue(new BigDecimal("100000")) 
                .build();

        RateBookDto.RateRuleDto baseRule = RateBookDto.RateRuleDto.builder()
                .id(1L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.BASE_PREMIUM)
                .category("PRIVATE_CAR")
                .priority(10)
                .build();

        RateBookDto.RateRuleDto minRule = RateBookDto.RateRuleDto.builder()
                .id(2L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.MIN_PREMIUM)
                .category("PRIVATE_CAR")
                .priority(20)
                .build();

        RateBookDto rb = RateBookDto.builder()
                .id(1L)
                .tenantId(tenantId)
                .rules(List.of(baseRule, minRule))
                .build();

        when(snapshotLoader.loadActive(tenantId)).thenReturn(RateBookSnapshotLoader.Snapshot.from(rb));
        when(ruleMatcher.matches(any(), any())).thenReturn(true);
        when(ruleMatcher.evaluateBigDecimal(eq(baseRule), any())).thenReturn(new BigDecimal("0.05")); // 5000
        when(ruleMatcher.evaluateBigDecimal(eq(minRule), any())).thenReturn(new BigDecimal("15000")); // Min 15000

        // when
        PricingResult result = pricingEngine.price(context);

        // then
        assertThat(result.getBasePremium()).isEqualByComparingTo("15000");
        assertThat(result.isMinimumPremiumApplied()).isTrue();
    }

    @Test
    void price_detectsReferral() {
        // given
        String tenantId = "TENANT1";
        RatingContext context = RatingContext.builder()
                .tenantId(tenantId)
                .category("PRIVATE_CAR")
                .vehicleValue(new BigDecimal("1000000"))
                .build();

        RateBookDto.RateRuleDto baseRule = RateBookDto.RateRuleDto.builder()
                .id(1L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.BASE_PREMIUM)
                .category("PRIVATE_CAR")
                .priority(10)
                .build();

        RateBookDto.RateRuleDto referralRule = RateBookDto.RateRuleDto.builder()
                .id(3L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.REFERRAL)
                .category("PRIVATE_CAR")
                .description("Refer old cars")
                .priority(5)
                .build();

        RateBookDto rb = RateBookDto.builder()
                .id(1L)
                .tenantId(tenantId)
                .rules(List.of(baseRule, referralRule))
                .build();

        when(snapshotLoader.loadActive(tenantId)).thenReturn(RateBookSnapshotLoader.Snapshot.from(rb));
        when(ruleMatcher.matches(eq(referralRule), any())).thenReturn(true);
        when(ruleMatcher.matches(eq(baseRule), any())).thenReturn(true);
        when(ruleMatcher.evaluateBigDecimal(eq(baseRule), any())).thenReturn(new BigDecimal("0.05"));

        // when
        PricingResult result = pricingEngine.price(context);

        // then
        assertThat(result.getReferralDecision()).isEqualTo(ReferralDecision.REFERRED);
        assertThat(result.getReferralReason()).isEqualTo("Refer old cars");
    }
}
