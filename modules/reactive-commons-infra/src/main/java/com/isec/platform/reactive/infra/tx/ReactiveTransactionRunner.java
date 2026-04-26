package com.isec.platform.reactive.infra.tx;

import reactor.core.publisher.Mono;

public interface ReactiveTransactionRunner {
    <T> Mono<T> inTransaction(Mono<T> operation);
}
