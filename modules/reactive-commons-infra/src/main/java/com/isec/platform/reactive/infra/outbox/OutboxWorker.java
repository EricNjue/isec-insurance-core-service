package com.isec.platform.reactive.infra.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private final OutboxRepository outboxRepository;
    private final List<OutboxEventHandler> handlers;

    @Scheduled(fixedDelayString = "${infra.outbox.poll-interval-ms:5000}")
    public void processOutboxEvents() {
        outboxRepository.findEventsToProcess(5, 10)
                .flatMap(this::processEvent)
                .subscribe();
    }

    private Mono<Void> processEvent(OutboxEvent event) {
        log.debug("Processing outbox event: {}", event.getId());
        
        return findHandler(event.getType())
                .handle(event)
                .then(markAsProcessed(event))
                .onErrorResume(e -> markAsFailed(event, e.getMessage()));
    }

    private OutboxEventHandler findHandler(String type) {
        return handlers.stream()
                .filter(h -> h.canHandle(type))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No handler found for event type: " + type));
    }

    private Mono<Void> markAsProcessed(OutboxEvent event) {
        event.setStatus(OutboxEvent.OutboxStatus.PROCESSED);
        event.setLastAttemptAt(LocalDateTime.now());
        return outboxRepository.save(event).then();
    }

    private Mono<Void> markAsFailed(OutboxEvent event, String error) {
        log.error("Failed to process outbox event {}: {}", event.getId(), error);
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastAttemptAt(LocalDateTime.now());
        event.setErrorMessage(error);
        
        if (event.getRetryCount() >= 5) {
            event.setStatus(OutboxEvent.OutboxStatus.FAILED);
        }
        
        return outboxRepository.save(event).then();
    }
}
