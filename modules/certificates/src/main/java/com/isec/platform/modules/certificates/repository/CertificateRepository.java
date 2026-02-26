package com.isec.platform.modules.certificates.repository;

import com.isec.platform.modules.certificates.domain.entity.CertificateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<CertificateEntity, UUID> {
    Optional<CertificateEntity> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);
    Optional<CertificateEntity> findByTenantIdAndExternalReference(String tenantId, String externalReference);
}
