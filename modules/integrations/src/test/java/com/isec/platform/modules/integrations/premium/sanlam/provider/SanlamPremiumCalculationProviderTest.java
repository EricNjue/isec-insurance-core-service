package com.isec.platform.modules.integrations.premium.sanlam.provider;

import com.isec.platform.modules.integrations.premium.model.PremiumCalculationRequest;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationResponse;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationStatus;
import com.isec.platform.modules.integrations.premium.provider.PremiumProviderType;
import com.isec.platform.modules.integrations.premium.sanlam.client.SanlamPremiumCalculationClient;
import com.isec.platform.modules.integrations.premium.sanlam.dto.SanlamCalculatePremiumRequest;
import com.isec.platform.modules.integrations.premium.sanlam.dto.SanlamCalculatePremiumResponse;
import com.isec.platform.modules.integrations.premium.sanlam.mapper.SanlamPremiumCalculationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SanlamPremiumCalculationProviderTest {

    @Mock
    private SanlamPremiumCalculationClient client;

    @Mock
    private SanlamPremiumCalculationMapper mapper;

    @InjectMocks
    private SanlamPremiumCalculationProvider provider;

    private PremiumCalculationRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = PremiumCalculationRequest.builder()
                .provider(PremiumProviderType.SANLAM)
                .rateType("new_business")
                .vehicleValue(new BigDecimal("500000"))
                .vehicleMake("AUDI")
                .vehicleModel("Q7")
                .vehicleYear(2018)
                .motorClass("private")
                .build();
    }

    @Test
    void shouldCalculatePremiumSuccessfully() {
        SanlamCalculatePremiumRequest sanlamRequest = new SanlamCalculatePremiumRequest();
        SanlamCalculatePremiumResponse sanlamResponse = new SanlamCalculatePremiumResponse();
        PremiumCalculationResponse expectedResponse = PremiumCalculationResponse.builder()
                .status(PremiumCalculationStatus.SUCCESS)
                .build();

        when(mapper.toSanlamRequest(validRequest)).thenReturn(sanlamRequest);
        when(client.calculatePremium(sanlamRequest)).thenReturn(Mono.just(sanlamResponse));
        when(mapper.toCommonResponse(sanlamResponse)).thenReturn(expectedResponse);

        StepVerifier.create(provider.calculatePremium(validRequest))
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(client).calculatePremium(sanlamRequest);
    }

    @Test
    void shouldFailValidationWhenVehicleValueIsZero() {
        validRequest.setVehicleValue(BigDecimal.ZERO);

        StepVerifier.create(provider.calculatePremium(validRequest))
                .expectError(IllegalArgumentException.class)
                .verify();

        verifyNoInteractions(client);
    }

    @Test
    void shouldFailValidationWhenVehicleMakeIsBlank() {
        validRequest.setVehicleMake("");

        StepVerifier.create(provider.calculatePremium(validRequest))
                .expectError(IllegalArgumentException.class)
                .verify();

        verifyNoInteractions(client);
    }

    @Test
    void shouldFailValidationWhenVehicleYearIsTooOld() {
        validRequest.setVehicleYear(1899);

        StepVerifier.create(provider.calculatePremium(validRequest))
                .expectError(IllegalArgumentException.class)
                .verify();

        verifyNoInteractions(client);
    }
}
