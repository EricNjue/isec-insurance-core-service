package com.isec.platform.reactive.infra.resilience;

import reactor.util.retry.Retry;

import java.time.Duration;

public class ResilienceUtils {

    public static Retry exponentialBackoff(int maxAttempts, Duration minBackoff) {
        return Retry.backoff(maxAttempts, minBackoff)
                .filter(throwable -> !(throwable instanceof IllegalArgumentException)) // Example: don't retry on validation errors
                .doBeforeRetry(retrySignal -> {
                    // Log retry attempt if needed
                });
    }
}
