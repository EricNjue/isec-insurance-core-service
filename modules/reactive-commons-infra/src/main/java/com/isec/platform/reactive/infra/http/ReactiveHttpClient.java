package com.isec.platform.reactive.infra.http;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ReactiveHttpClient {

    private final WebClient webClient;

    public ReactiveHttpClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public <T> Mono<T> get(String url, Class<T> responseType) {
        return get(url, responseType, HttpClientOptions.defaultOptions());
    }

    public <T> Mono<T> get(String url, Class<T> responseType, HttpClientOptions options) {
        WebClient.RequestHeadersSpec<?> request = webClient.get()
                .uri(url);
        
        if (options.getHeaders() != null) {
            request.headers(options.getHeaders());
        }

        Mono<T> mono = request.retrieve()
                .bodyToMono(responseType)
                .timeout(options.getTimeout());
        
        if (options.getRetrySpec() != null) {
            mono = mono.retryWhen(options.getRetrySpec());
        }
        
        return mono;
    }

    public <T> Mono<T> post(String url, Object body, Class<T> responseType) {
        return post(url, body, responseType, HttpClientOptions.defaultOptions());
    }

    public <T> Mono<T> post(String url, Object body, Class<T> responseType, HttpClientOptions options) {
        WebClient.RequestBodySpec request = webClient.post()
                .uri(url);

        if (options.getHeaders() != null) {
            request.headers(options.getHeaders());
        }

        Mono<T> mono = request.bodyValue(body)
                .retrieve()
                .bodyToMono(responseType)
                .timeout(options.getTimeout());

        if (options.getRetrySpec() != null) {
            mono = mono.retryWhen(options.getRetrySpec());
        }

        return mono;
    }

    public <T> Mono<T> put(String url, Object body, Class<T> responseType) {
        return put(url, body, responseType, HttpClientOptions.defaultOptions());
    }

    public <T> Mono<T> put(String url, Object body, Class<T> responseType, HttpClientOptions options) {
        WebClient.RequestBodySpec request = webClient.put()
                .uri(url);

        if (options.getHeaders() != null) {
            request.headers(options.getHeaders());
        }

        Mono<T> mono = request.bodyValue(body)
                .retrieve()
                .bodyToMono(responseType)
                .timeout(options.getTimeout());

        if (options.getRetrySpec() != null) {
            mono = mono.retryWhen(options.getRetrySpec());
        }

        return mono;
    }

    public Mono<Void> delete(String url) {
        return delete(url, HttpClientOptions.defaultOptions());
    }

    public Mono<Void> delete(String url, HttpClientOptions options) {
        WebClient.RequestHeadersSpec<?> request = webClient.delete()
                .uri(url);

        if (options.getHeaders() != null) {
            request.headers(options.getHeaders());
        }

        Mono<Void> mono = request.retrieve()
                .bodyToMono(Void.class)
                .timeout(options.getTimeout());

        if (options.getRetrySpec() != null) {
            mono = mono.retryWhen(options.getRetrySpec());
        }

        return mono;
    }
}
