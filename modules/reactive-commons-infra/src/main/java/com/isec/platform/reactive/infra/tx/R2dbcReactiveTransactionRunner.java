package com.isec.platform.reactive.infra.tx;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class R2dbcReactiveTransactionRunner implements ReactiveTransactionRunner {

    private final TransactionalOperator transactionalOperator;

    @Override
    public <T> Mono<T> inTransaction(Mono<T> operation) {
        return operation.as(transactionalOperator::transactional);
    }
}
