# Reactive Commons Infrastructure

The `reactive-commons-infra` module provides a set of reusable, non-blocking infrastructure utilities designed for Spring Boot reactive applications. It centralizes common "plumbing" tasks such as HTTP communication, database transactions, and reliability patterns, allowing service modules to focus on business logic.

## Key Features

- **Reactive HTTP Client**: A generic, configurable wrapper around Spring's `WebClient`.
- **Reactive Transaction Runner**: Utilities for managing R2DBC transactions programmatically.
- **Execution Strategies**: Abstractions for immediate execution vs. reliable outbox-based execution.
- **Outbox Pattern**: Full implementation of the Transactional Outbox pattern for guaranteed message delivery.
- **Resilience**: Built-in support for retries with exponential backoff and timeouts.
- **Observability**: Automatic Correlation ID propagation across reactive flows.

---

## Tech Stack

- **Java 21**
- **Spring Boot 3.3+**
- **Project Reactor** (Mono/Flux)
- **Spring Data R2DBC** (PostgreSQL)
- **Spring WebFlux**

---

## Capabilities & Usage

### 1. Reactive HTTP Client (`ReactiveHttpClient`)

A reusable wrapper for making external API calls with built-in support for timeouts, retries, and header injection.

**Example Usage:**

```java
@Service
@RequiredArgsConstructor
public class ExternalService {
    private final ReactiveHttpClient httpClient;

    public Mono<UserResponse> getUser(String userId) {
        return httpClient.get(
            "https://api.example.com/users/" + userId, 
            UserResponse.class
        );
    }

    public Mono<Void> postData(DataRequest request) {
        HttpClientOptions options = HttpClientOptions.builder()
            .timeout(Duration.ofSeconds(5))
            .retrySpec(Retry.backoff(2, Duration.ofMillis(500)))
            .headers(h -> h.setBearerAuth("token"))
            .build();

        return httpClient.post(
            "https://api.example.com/data", 
            request, 
            Void.class, 
            options
        );
    }
}
```

### 2. Reactive Transaction Runner (`ReactiveTransactionRunner`)

Ensures that multiple database operations are executed within the same R2DBC transaction.

**Example Usage:**

```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final ReactiveTransactionRunner txRunner;
    private final MyRepository repository;

    public Mono<Void> saveComplexData(Data data) {
        return txRunner.inTransaction(
            repository.save(data.getPart1())
                .then(repository.save(data.getPart2()))
                .then()
        );
    }
}
```

### 3. Execution Strategy Abstraction

We provide `ReactiveOperationExecutor` with two primary implementations to handle DB operations followed by side effects (like HTTP calls).

#### a) Direct Execution (`DirectReactiveExecutor`)
Used for low-risk flows where the side effect is executed immediately after the DB operation.

#### b) Outbox Execution (`OutboxReactiveExecutor`)
Used for critical flows. The DB operation and an outbox event are saved in the same transaction. An async worker then processes the event.

**Example Usage (from PaymentServiceExample):**

```java
// Injecting both strategies
@Qualifier("directReactiveExecutor")
private final ReactiveOperationExecutor directExecutor;

@Qualifier("outboxReactiveExecutor")
private final ReactiveOperationExecutor outboxExecutor;

// 1. Direct Execution
public Mono<Payment> processPaymentDirect(PaymentRequest request) {
    return directExecutor.executeDirect(
            paymentRepository.save(createPayment(request)),
            () -> httpClient.post("https://gateway.com/pay", request, String.class)
    );
}

// 2. Outbox Execution
public Mono<Payment> processPaymentWithOutbox(PaymentRequest request) {
    OutboxEvent event = OutboxEvent.builder()
            .type("PAYMENT_CREATED")
            .payload(serialize(request))
            .idempotencyKey(UUID.randomUUID().toString())
            .build();

    return outboxExecutor.executeWithOutbox(
            paymentRepository.save(createPayment(request)),
            event
    );
}
```

### 4. Transactional Outbox Pattern

The module includes an `OutboxWorker` that automatically polls `PENDING` or `FAILED` events and processes them using registered `OutboxEventHandler` implementations.

**To implement a handler:**

```java
@Component
public class MyPaymentEventHandler implements OutboxEventHandler {
    @Override
    public boolean canHandle(String eventType) {
        return "PAYMENT_CREATED".equals(eventType);
    }

    @Override
    public Mono<Void> handle(OutboxEvent event) {
        // Logic to process the event (e.g., call an external API)
        return Mono.empty();
    }
}
```

### 5. Resilience Utilities (`ResilienceUtils`)

Provides standardized retry policies.

```java
Mono.from(operation)
    .retryWhen(ResilienceUtils.exponentialBackoff(3, Duration.ofSeconds(1)))
```

### 6. Observability

The `CorrelationIdFilter` automatically captures or generates an `X-Correlation-ID` header and propagates it through the Reactor `Context`. This ensures that logs across different services/threads can be correlated.

---

## Configuration

The module is automatically configured via `ReactiveInfraConfig`. Ensure your main application scans the `com.isec.platform.reactive.infra` package or imports the configuration class.

Required properties (example):
```yaml
# No specific properties required for core, but WebClient and R2DBC must be configured in the main app.
```

## Database Setup

The Outbox pattern requires an `outbox_events` table. 
- **Liquibase**: A changelog is provided in the `app-bootstrap` module (`38-reactive-commons.yaml`).
- **Manual**: See [README-DB.md](./README-DB.md) for SQL scripts.

---
