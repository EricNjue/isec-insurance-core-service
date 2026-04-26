package com.isec.platform.reactive.infra.outbox;

import reactor.core.publisher.Mono;

public interface OutboxEventHandler {
    boolean canHandle(String eventType);
    Mono<Void> handle(OutboxEvent event);
}
