package com.isec.platform.reactive.infra.executor;

import com.isec.platform.reactive.infra.outbox.OutboxEvent;
import com.isec.platform.reactive.infra.tx.ReactiveTransactionRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class DirectReactiveExecutor implements ReactiveOperationExecutor {

    private final ReactiveTransactionRunner transactionRunner;

    @Override
    public <T> Mono<T> executeDirect(Mono<T> dbOperation, Supplier<Mono<?>> httpOperation) {
        // Execute DB operation in transaction, then HTTP operation
        return transactionRunner.inTransaction(dbOperation)
                .flatMap(result -> httpOperation.get().thenReturn(result));
    }

    @Override
    public <T> Mono<T> executeWithOutbox(Mono<T> dbOperation, OutboxEvent event) {
        throw new UnsupportedOperationException("Direct executor does not support outbox");
    }
}
