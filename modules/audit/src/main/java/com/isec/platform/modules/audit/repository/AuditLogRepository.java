package com.isec.platform.modules.audit.repository;

import com.isec.platform.modules.audit.domain.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface AuditLogRepository extends ReactiveCrudRepository<AuditLog, Long> {
    Flux<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);
}
