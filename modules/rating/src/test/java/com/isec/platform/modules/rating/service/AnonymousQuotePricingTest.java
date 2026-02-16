package com.isec.platform.modules.rating.service;

import com.isec.platform.modules.rating.domain.AnonymousQuote;
import com.isec.platform.modules.rating.dto.AnonymousQuoteRequest;
import com.isec.platform.modules.rating.dto.PricingResult;
import com.isec.platform.modules.rating.repository.AnonymousQuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AnonymousQuotePricingTest {

    private AnonymousQuoteRepository anonymousQuoteRepository;
    private PricingEngine pricingEngine;
    private RatingService ratingService;

    @BeforeEach
    void setUp() {
        anonymousQuoteRepository = Mockito.mock(AnonymousQuoteRepository.class);
        pricingEngine = Mockito.mock(PricingEngine.class);
        ratingService = new RatingService(anonymousQuoteRepository, pricingEngine);
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

        when(pricingEngine.price(any())).thenReturn(pricingResult);
        when(anonymousQuoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        AnonymousQuote result = ratingService.createAnonymousQuote(request);

        // then
        assertThat(result.getPremiumBreakdown().basePremium()).isEqualByComparingTo("37500");
        assertThat(result.getPremiumBreakdown().totalPremium()).isEqualByComparingTo("37708.75");
    }
}
