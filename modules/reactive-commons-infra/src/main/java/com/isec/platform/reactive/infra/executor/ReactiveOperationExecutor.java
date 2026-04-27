package com.isec.platform.reactive.infra.executor;

import com.isec.platform.reactive.infra.outbox.OutboxEvent;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

public interface ReactiveOperationExecutor {

    <T> Mono<T> executeDirect(
            Mono<T> dbOperation,
            Supplier<Mono<?>> httpOperation
    );

    <T> Mono<T> executeWithOutbox(
            Mono<T> dbOperation,
            OutboxEvent event
    );
}
