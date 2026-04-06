package com.isec.platform.modules.certificates.repository;

import com.isec.platform.modules.certificates.domain.CertificateIngestionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificateIngestionAuditRepository extends JpaRepository<CertificateIngestionAudit, Long> {
    Optional<CertificateIngestionAudit> findByEmailMessageId(String emailMessageId);
    boolean existsByEmailMessageId(String emailMessageId);
}
