package com.isec.platform.common.idempotency.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";

    public Mono<Boolean> isDuplicate(String key) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + key;
        return Mono.fromCallable(() -> redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSED", Duration.ofDays(7)))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(isNew -> {
                    boolean duplicate = isNew == null || !isNew;
                    if (duplicate) {
                        log.warn("Duplicate request detected for key: {}", key);
                    }
                    return duplicate;
                });
    }
    
    public Mono<Void> markAsProcessed(String key) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + key;
        return Mono.fromRunnable(() -> redisTemplate.opsForValue().set(redisKey, "PROCESSED", Duration.ofDays(7)))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .then();
    }
}
