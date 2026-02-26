package com.isec.platform.modules.certificates.repository;

import com.isec.platform.modules.certificates.domain.canonical.ProviderType;
import com.isec.platform.modules.certificates.domain.entity.TenantProviderMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantProviderMappingRepository extends JpaRepository<TenantProviderMappingEntity, UUID> {
    Optional<TenantProviderMappingEntity> findByTenantIdAndProviderCodeAndActiveTrue(String tenantId, ProviderType providerType);
    Optional<TenantProviderMappingEntity> findFirstByTenantIdAndActiveTrueOrderByIdAsc(String tenantId);
}
