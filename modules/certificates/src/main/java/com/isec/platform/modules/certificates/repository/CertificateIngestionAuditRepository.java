package com.isec.platform.modules.certificates.repository;

import com.isec.platform.modules.certificates.domain.CertificateIngestionAudit;
import com.isec.platform.modules.certificates.domain.IngestionStatus;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface CertificateIngestionAuditRepository extends ReactiveCrudRepository<CertificateIngestionAudit, Long> {
    Mono<CertificateIngestionAudit> findByEmailMessageId(String emailMessageId);
    Mono<Boolean> existsByEmailMessageId(String emailMessageId);

    @Modifying
    @Query("UPDATE certificate_ingestion_audit SET status = :newStatus WHERE email_message_id = :emailMessageId AND status = :oldStatus")
    Mono<Integer> updateStatusAtomic(@Param("emailMessageId") String emailMessageId, @Param("oldStatus") String oldStatus, @Param("newStatus") String newStatus);

    Flux<CertificateIngestionAudit> findByStatusAndCreatedAtBefore(IngestionStatus status, LocalDateTime threshold);
}
