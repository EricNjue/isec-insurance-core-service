package com.isec.platform.modules.certificates.repository;

import com.isec.platform.modules.certificates.domain.CertificateIngestionAudit;
import com.isec.platform.modules.certificates.domain.IngestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateIngestionAuditRepository extends JpaRepository<CertificateIngestionAudit, Long> {
    Optional<CertificateIngestionAudit> findByEmailMessageId(String emailMessageId);
    boolean existsByEmailMessageId(String emailMessageId);

    @Modifying
    @Query("UPDATE CertificateIngestionAudit a SET a.status = :newStatus WHERE a.emailMessageId = :emailMessageId AND a.status = :oldStatus")
    int updateStatusAtomic(@Param("emailMessageId") String emailMessageId, @Param("oldStatus") IngestionStatus oldStatus, @Param("newStatus") IngestionStatus newStatus);

    List<CertificateIngestionAudit> findByStatusAndCreatedAtBefore(IngestionStatus status, LocalDateTime threshold);
}
