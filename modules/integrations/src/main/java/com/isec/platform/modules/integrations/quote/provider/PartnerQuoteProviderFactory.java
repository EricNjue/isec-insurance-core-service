package com.isec.platform.modules.integrations.quote.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PartnerQuoteProviderFactory {

    private final Map<PartnerType, PartnerQuoteProvider> providers;

    public PartnerQuoteProviderFactory(List<PartnerQuoteProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(PartnerQuoteProvider::providerType, provider -> provider));
    }

    public PartnerQuoteProvider getProvider(PartnerType providerType) {
        return Optional.ofNullable(providers.get(providerType))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported partner quote provider: " + providerType));
    }
}
