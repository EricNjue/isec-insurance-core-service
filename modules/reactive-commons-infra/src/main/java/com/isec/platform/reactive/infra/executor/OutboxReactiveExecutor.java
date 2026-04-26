package com.isec.platform.reactive.infra.executor;

import com.isec.platform.reactive.infra.outbox.OutboxEvent;
import com.isec.platform.reactive.infra.outbox.OutboxRepository;
import com.isec.platform.reactive.infra.tx.ReactiveTransactionRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class OutboxReactiveExecutor implements ReactiveOperationExecutor {

    private final ReactiveTransactionRunner transactionRunner;
    private final OutboxRepository outboxRepository;

    @Override
    public <T> Mono<T> executeDirect(Mono<T> dbOperation, Supplier<Mono<?>> httpOperation) {
        throw new UnsupportedOperationException("Outbox executor does not support direct execution");
    }

    @Override
    public <T> Mono<T> executeWithOutbox(Mono<T> dbOperation, OutboxEvent event) {
        // Initialize event metadata if not present
        if (event.getId() == null) event.setId(UUID.randomUUID());
        if (event.getCreatedAt() == null) event.setCreatedAt(LocalDateTime.now());
        if (event.getStatus() == null) event.setStatus(OutboxEvent.OutboxStatus.PENDING);
        if (event.getRetryCount() == null) event.setRetryCount(0);

        // Execute both in the same transaction
        Mono<T> combinedOperation = dbOperation
                .flatMap(result -> outboxRepository.save(event).thenReturn(result));

        return transactionRunner.inTransaction(combinedOperation);
    }
}
