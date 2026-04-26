package com.isec.platform.modules.rating.service;

import com.isec.platform.modules.rating.domain.AnonymousQuote;
import com.isec.platform.modules.rating.dto.AnonymousQuoteRequest;
import com.isec.platform.modules.rating.dto.PricingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AnonymousQuotePricingTest {

    private ReactiveRedisTemplate<String, Object> redisTemplate;
    private PricingEngine pricingEngine;
    private RatingService ratingService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = Mockito.mock(ReactiveRedisTemplate.class);
        ReactiveValueOperations<String, Object> valueOps = Mockito.mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        pricingEngine = Mockito.mock(PricingEngine.class);
        ratingService = new RatingService(redisTemplate, pricingEngine);
    }

    @Test
    void createAnonymousQuote_usesPricingEngine() {
        // given
        AnonymousQuoteRequest request = AnonymousQuoteRequest.builder()
                .vehicleMake("Toyota")
                .vehicleModel("Vitz")
                .yearOfManufacture(2018)
                .vehicleValue(new BigDecimal("400000"))
                .baseRate(new BigDecimal("0.06"))
                .build();

        PricingResult pricingResult = PricingResult.builder()
                .basePremium(new BigDecimal("37500"))
                .pcf(new BigDecimal("93.75"))
                .itl(new BigDecimal("75.00"))
                .certificateCharge(new BigDecimal("40.00"))
                .totalPremium(new BigDecimal("37708.75"))
                .build();

        when(pricingEngine.price(any())).thenReturn(Mono.just(pricingResult));
        when(redisTemplate.opsForValue().set(anyString(), any(), any())).thenReturn(Mono.just(true));

        // when & then
        ratingService.createAnonymousQuote(request)
                .as(StepVerifier::create)
                .consumeNextWith(result -> {
                    assertThat(result.getPremiumBreakdown().basePremium()).isEqualByComparingTo("37500");
                    assertThat(result.getPremiumBreakdown().totalPremium()).isEqualByComparingTo("37708.75");
                })
                .verifyComplete();
    }
}
