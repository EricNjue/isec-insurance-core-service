package com.isec.platform.modules.certificates.repository;

import com.isec.platform.modules.certificates.domain.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByPolicyId(Long policyId);
    Optional<Certificate> findByIdempotencyKey(String idempotencyKey);
}
