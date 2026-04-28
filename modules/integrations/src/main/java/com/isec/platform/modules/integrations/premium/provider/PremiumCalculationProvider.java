package com.isec.platform.modules.integrations.premium.provider;

import com.isec.platform.modules.integrations.premium.model.PremiumCalculationRequest;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationResponse;
import reactor.core.publisher.Mono;

public interface PremiumCalculationProvider {

    PremiumProviderType providerType();

    Mono<PremiumCalculationResponse> calculatePremium(PremiumCalculationRequest request);
}
