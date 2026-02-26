package com.isec.platform.modules.certificates.config;

import com.isec.platform.modules.certificates.domain.canonical.ProviderType;
import com.isec.platform.modules.certificates.domain.entity.InsuranceProviderEntity;
import com.isec.platform.modules.certificates.exception.ProviderMappingException;
import com.isec.platform.modules.certificates.repository.InsuranceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProviderConfigurationService {

    private final InsuranceProviderRepository insuranceProviderRepository;

    public ProviderConfiguration getConfiguration(ProviderType providerType) {
        InsuranceProviderEntity provider = insuranceProviderRepository.findByProviderCode(providerType)
                .orElseThrow(() -> new ProviderMappingException("Provider configuration not found for " + providerType));

        if (Boolean.FALSE.equals(provider.getActive())) {
            throw new ProviderMappingException("Provider is inactive: " + providerType);
        }

        return new ProviderConfiguration(
                provider.getProviderCode(),
                provider.getBaseUrl(),
                provider.getAuthType(),
                provider.getTimeoutMs() == null ? 5000 : provider.getTimeoutMs(),
                provider.getRetryCount() == null ? 0 : provider.getRetryCount(),
                Boolean.TRUE.equals(provider.getActive())
        );
    }
}
