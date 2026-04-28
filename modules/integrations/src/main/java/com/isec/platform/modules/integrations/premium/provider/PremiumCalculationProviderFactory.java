package com.isec.platform.modules.integrations.premium.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PremiumCalculationProviderFactory {

    private final Map<PremiumProviderType, PremiumCalculationProvider> providers;

    public PremiumCalculationProviderFactory(List<PremiumCalculationProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(PremiumCalculationProvider::providerType, provider -> provider));
    }

    public PremiumCalculationProvider getProvider(PremiumProviderType providerType) {
        return Optional.ofNullable(providers.get(providerType))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported premium provider: " + providerType));
    }
}
