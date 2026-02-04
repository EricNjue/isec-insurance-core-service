package com.isec.platform.common.idempotency.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";

    public boolean isDuplicate(String key) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + key;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSED", Duration.ofDays(7));
        boolean duplicate = isNew == null || !isNew;
        if (duplicate) {
            log.warn("Duplicate request detected for key: {}", key);
        }
        return duplicate;
    }
    
    public void markAsProcessed(String key) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + key;
        redisTemplate.opsForValue().set(redisKey, "PROCESSED", Duration.ofDays(7));
    }
}
