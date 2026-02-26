package com.isec.platform.modules.certificates.repository;

import com.isec.platform.modules.certificates.domain.canonical.ProviderType;
import com.isec.platform.modules.certificates.domain.entity.InsuranceProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InsuranceProviderRepository extends JpaRepository<InsuranceProviderEntity, UUID> {
    Optional<InsuranceProviderEntity> findByProviderCode(ProviderType providerType);
    Optional<InsuranceProviderEntity> findFirstByActiveTrueOrderByCreatedAtAsc();
}
