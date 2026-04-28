package com.isec.platform.modules.integrations.mpesa.provider;

import com.isec.platform.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MpesaProviderFactory {

    private final Map<MpesaProviderType, MpesaPaymentProvider> providers;

    public MpesaProviderFactory(List<MpesaPaymentProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(MpesaPaymentProvider::providerType, Function.identity()));
    }

    public MpesaPaymentProvider getProvider(MpesaProviderType type) {
        return Optional.ofNullable(providers.get(type))
                .orElseThrow(() -> new BusinessException("Unsupported M-Pesa provider: " + type));
    }
}
