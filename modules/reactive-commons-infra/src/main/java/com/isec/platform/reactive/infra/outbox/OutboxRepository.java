package com.isec.platform.reactive.infra.outbox;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface OutboxRepository extends ReactiveCrudRepository<OutboxEvent, UUID> {
    
    @Query("SELECT * FROM outbox_events WHERE status = 'PENDING' OR (status = 'FAILED' AND retry_count < :maxRetries) ORDER BY created_at ASC LIMIT :limit")
    Flux<OutboxEvent> findEventsToProcess(int maxRetries, int limit);
}
