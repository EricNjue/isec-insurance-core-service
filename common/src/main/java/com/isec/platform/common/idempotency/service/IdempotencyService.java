package com.isec.platform.common.idempotency.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";

    public Mono<Boolean> isDuplicate(String key) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + key;
        return redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSED", Duration.ofDays(7))
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
        return redisTemplate.opsForValue().set(redisKey, "PROCESSED", Duration.ofDays(7))
                .then();
    }
}
