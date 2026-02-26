package com.isec.platform.modules.certificates.registry;

import com.isec.platform.modules.certificates.adapters.CertificateProviderAdapter;
import com.isec.platform.modules.certificates.domain.canonical.ProviderType;
import com.isec.platform.modules.certificates.domain.entity.InsuranceProviderEntity;
import com.isec.platform.modules.certificates.exception.ProviderMappingException;
import com.isec.platform.modules.certificates.repository.InsuranceProviderRepository;
import com.isec.platform.modules.certificates.repository.TenantProviderMappingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CertificateProviderRegistry {

    private final TenantProviderMappingRepository tenantProviderMappingRepository;
    private final InsuranceProviderRepository insuranceProviderRepository;
    private final Map<ProviderType, CertificateProviderAdapter> adapterMap;

    public CertificateProviderRegistry(List<CertificateProviderAdapter> adapters,
                                       TenantProviderMappingRepository tenantProviderMappingRepository,
                                       InsuranceProviderRepository insuranceProviderRepository) {
        this.tenantProviderMappingRepository = tenantProviderMappingRepository;
        this.insuranceProviderRepository = insuranceProviderRepository;
        this.adapterMap = adapters.stream()
                .collect(Collectors.toMap(CertificateProviderAdapter::providerType, adapter -> adapter));
    }

    public CertificateProviderAdapter resolveProvider(String tenantId, ProviderType requestedProvider) {
        // Strategy resolution: explicit provider -> tenant default -> global fallback.
        ProviderType providerType = requestedProvider != null
                ? resolveExplicitProvider(tenantId, requestedProvider)
                : resolveTenantDefaultProvider(tenantId);

        CertificateProviderAdapter adapter = adapterMap.get(providerType);
        if (adapter == null) {
            throw new ProviderMappingException("No adapter registered for provider " + providerType);
        }
        return adapter;
    }

    private ProviderType resolveExplicitProvider(String tenantId, ProviderType requestedProvider) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new ProviderMappingException("Tenant ID is required to resolve provider");
        }

        return tenantProviderMappingRepository.findByTenantIdAndProviderCodeAndActiveTrue(tenantId, requestedProvider)
                .map(mapping -> mapping.getProviderCode())
                .orElseThrow(() -> new ProviderMappingException("Provider " + requestedProvider + " is not mapped for tenant " + tenantId));
    }

    private ProviderType resolveTenantDefaultProvider(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new ProviderMappingException("Tenant ID is required to resolve provider");
        }

        Optional<ProviderType> tenantDefault = tenantProviderMappingRepository
                .findFirstByTenantIdAndActiveTrueOrderByIdAsc(tenantId)
                .map(mapping -> mapping.getProviderCode());

        if (tenantDefault.isPresent()) {
            return tenantDefault.get();
        }

        InsuranceProviderEntity fallback = insuranceProviderRepository.findFirstByActiveTrueOrderByCreatedAtAsc()
                .orElseThrow(() -> new ProviderMappingException("No active providers configured"));
        return fallback.getProviderCode();
    }
}
