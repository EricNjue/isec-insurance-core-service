package com.isec.platform.modules.integrations.premium.sanlam.provider;

import com.isec.platform.modules.integrations.premium.model.PremiumCalculationRequest;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationResponse;
import com.isec.platform.modules.integrations.premium.provider.PremiumCalculationProvider;
import com.isec.platform.modules.integrations.premium.provider.PremiumProviderType;
import com.isec.platform.modules.integrations.premium.sanlam.client.SanlamPremiumCalculationClient;
import com.isec.platform.modules.integrations.premium.sanlam.mapper.SanlamPremiumCalculationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class SanlamPremiumCalculationProvider implements PremiumCalculationProvider {

    private final SanlamPremiumCalculationClient client;
    private final SanlamPremiumCalculationMapper mapper;

    @Override
    public PremiumProviderType providerType() {
        return PremiumProviderType.SANLAM;
    }

    @Override
    public Mono<PremiumCalculationResponse> calculatePremium(PremiumCalculationRequest request) {
        return Mono.fromRunnable(() -> validateRequest(request))
                .then(Mono.defer(() -> {
                    long startTime = Instant.now().toEpochMilli();
                    log.info("Starting Sanlam premium calculation for vehicle: {} {} ({})",
                            request.getVehicleMake(), request.getVehicleModel(), request.getVehicleYear());

                    return client.calculatePremium(mapper.toSanlamRequest(request))
                            .map(mapper::toCommonResponse)
                            .doOnNext(response -> {
                                long latency = Instant.now().toEpochMilli() - startTime;
                                log.info("Sanlam premium calculation completed. Provider: {}, Latency: {}ms, Status: {}",
                                        providerType(), latency, response.getStatus());
                            })
                            .doOnError(error -> log.error("Sanlam premium calculation failed. Provider: {}, Error: {}",
                                    providerType(), error.getMessage()));
                }));
    }

    private void validateRequest(PremiumCalculationRequest request) {
        Assert.notNull(request, "PremiumCalculationRequest must not be null");
        Assert.notNull(request.getProvider(), "Provider must not be null");
        Assert.hasText(request.getRateType(), "Rate type must not be blank");
        Assert.notNull(request.getVehicleValue(), "Vehicle value must not be null");
        Assert.isTrue(request.getVehicleValue().compareTo(BigDecimal.ZERO) > 0, "Vehicle value must be greater than zero");
        Assert.hasText(request.getVehicleMake(), "Vehicle make must not be blank");
        Assert.hasText(request.getVehicleModel(), "Vehicle model must not be blank");
        Assert.notNull(request.getVehicleYear(), "Vehicle year must not be null");
        Assert.isTrue(request.getVehicleYear() > 1900, "Vehicle year must be reasonable");
        Assert.hasText(request.getMotorClass(), "Motor class must not be blank");
        
        if (request.getWindscreenBenefit() != null) {
            Assert.isTrue(request.getWindscreenBenefit().compareTo(BigDecimal.ZERO) >= 0, "Windscreen benefit must not be negative");
        }
        if (request.getRadioCassetteBenefit() != null) {
            Assert.isTrue(request.getRadioCassetteBenefit().compareTo(BigDecimal.ZERO) >= 0, "Radio/Cassette benefit must not be negative");
        }
    }
}
