package com.isec.platform.modules.rating.service;

import com.isec.platform.modules.rating.domain.RateBook;
import com.isec.platform.modules.rating.domain.RateRule;
import com.isec.platform.modules.rating.domain.RuleType;
import com.isec.platform.modules.rating.dto.PricingResult;
import com.isec.platform.modules.rating.dto.RatingContext;
import com.isec.platform.modules.rating.dto.ReferralDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingEngineTest {

    @Mock
    private RateBookSnapshotLoader rateBookSnapshotLoader;

    @Mock
    private RuleMatcher ruleMatcher;

    @InjectMocks
    private PricingEngine pricingEngine;

    private RateBook rateBook;
    private RatingContext context;

    @BeforeEach
    void setUp() {
        rateBook = RateBook.builder()
                .id(1L)
                .versionName("v1.0")
                .build();
        rateBook.setTenantId("SANLAM");

        context = RatingContext.builder()
                .tenantId("SANLAM")
                .category("PRIVATE_CAR")
                .vehicleValue(new BigDecimal("1000000"))
                .vehicleAge(5)
                .build();

        RateBookSnapshotLoader.Snapshot snapshot = new RateBookSnapshotLoader.Snapshot(1L, "v1.0", rateBook, "SANLAM:1:v1.0");
        when(rateBookSnapshotLoader.loadActive("SANLAM")).thenReturn(Optional.of(snapshot));
    }

    @Test
    void shouldCalculateBasePremium() {
        RateRule baseRule = RateRule.builder()
                .id(10L)
                .ruleType(RuleType.BASE_PREMIUM)
                .category("PRIVATE_CAR")
                .priority(10)
                .build();
        rateBook.setRules(List.of(baseRule));

        when(ruleMatcher.matches(eq(baseRule), any())).thenReturn(true);
        when(ruleMatcher.evaluateBigDecimal(eq(baseRule), any())).thenReturn(new BigDecimal("0.04"));

        PricingResult result = pricingEngine.price(context);

        assertNotNull(result);
        assertEquals(new BigDecimal("40000.00"), result.getBasePremium());
        assertEquals(new BigDecimal("100.00"), result.getPcf()); // 0.25% of 40000
        assertEquals(new BigDecimal("80.00"), result.getItl());  // 0.20% of 40000
        assertEquals(new BigDecimal("40.00"), result.getCertificateCharge());
        assertEquals(new BigDecimal("40220.00"), result.getTotalPremium());
        assertEquals(ReferralDecision.NONE, result.getReferralDecision());
    }

    @Test
    void shouldApplyMinimumPremium() {
        RateRule baseRule = RateRule.builder()
                .id(10L)
                .ruleType(RuleType.BASE_PREMIUM)
                .category("PRIVATE_CAR")
                .priority(10)
                .build();
        RateRule minRule = RateRule.builder()
                .id(11L)
                .ruleType(RuleType.MIN_PREMIUM)
                .category("PRIVATE_CAR")
                .priority(20)
                .build();
        rateBook.setRules(List.of(baseRule, minRule));

        when(ruleMatcher.matches(any(), any())).thenReturn(true);
        when(ruleMatcher.evaluateBigDecimal(eq(baseRule), any())).thenReturn(new BigDecimal("0.01")); // 1% of 1M = 10000
        when(ruleMatcher.evaluateBigDecimal(eq(minRule), any())).thenReturn(new BigDecimal("15000"));

        PricingResult result = pricingEngine.price(context);

        assertTrue(result.isMinimumPremiumApplied());
        assertEquals(0, result.getBasePremium().compareTo(new BigDecimal("15000.00")));
    }

    @Test
    void shouldTriggerReferral() {
        RateRule baseRule = RateRule.builder()
                .id(10L)
                .ruleType(RuleType.BASE_PREMIUM)
                .category("PRIVATE_CAR")
                .priority(10)
                .build();
        RateRule referralRule = RateRule.builder()
                .id(12L)
                .ruleType(RuleType.REFERRAL)
                .category("PRIVATE_CAR")
                .priority(5)
                .description("Refer old vehicles")
                .build();
        rateBook.setRules(List.of(baseRule, referralRule));

        when(ruleMatcher.matches(eq(baseRule), any())).thenReturn(true);
        when(ruleMatcher.matches(eq(referralRule), any())).thenReturn(true);
        when(ruleMatcher.evaluateBigDecimal(eq(baseRule), any())).thenReturn(new BigDecimal("0.04"));

        PricingResult result = pricingEngine.price(context);

        assertEquals(ReferralDecision.REFERRED, result.getReferralDecision());
        assertEquals("Refer old vehicles", result.getReferralReason());
    }
}
