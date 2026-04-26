package com.isec.platform.reactive.infra.http;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.function.Consumer;

@Getter
@Builder
public class HttpClientOptions {
    private final Duration timeout;
    private final Retry retrySpec;
    private final Consumer<HttpHeaders> headers;

    public static HttpClientOptions defaultOptions() {
        return HttpClientOptions.builder()
                .timeout(Duration.ofSeconds(10))
                .retrySpec(Retry.backoff(3, Duration.ofSeconds(1)))
                .build();
    }
}
