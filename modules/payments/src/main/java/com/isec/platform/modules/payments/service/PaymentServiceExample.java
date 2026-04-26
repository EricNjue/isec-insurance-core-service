package com.isec.platform.modules.payments.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.modules.payments.dto.PaymentRequest;
import com.isec.platform.modules.payments.entity.Payment;
import com.isec.platform.modules.payments.repository.reactive.ReactivePaymentRepository;
import com.isec.platform.reactive.infra.executor.ReactiveOperationExecutor;
import com.isec.platform.reactive.infra.http.ReactiveHttpClient;
import com.isec.platform.reactive.infra.outbox.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceExample {

    private final ReactivePaymentRepository paymentRepository;
    private final ReactiveHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Qualifier("directReactiveExecutor")
    private final ReactiveOperationExecutor directExecutor;

    @Qualifier("outboxReactiveExecutor")
    private final ReactiveOperationExecutor outboxExecutor;

    public Mono<Payment> processPaymentDirect(PaymentRequest request) {
        Payment payment = createPaymentEntity(request);

        return directExecutor.executeDirect(
                paymentRepository.save(payment),
                () -> httpClient.post("https://payment-gateway.com/pay", request, String.class)
        );
    }

    @SneakyThrows
    public Mono<Payment> processPaymentWithOutbox(PaymentRequest request) {
        Payment payment = createPaymentEntity(request);

        OutboxEvent event = OutboxEvent.builder()
                .type("PAYMENT_CREATED")
                .payload(objectMapper.writeValueAsString(request))
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        return outboxExecutor.executeWithOutbox(
                paymentRepository.save(payment),
                event
        );
    }

    private Payment createPaymentEntity(PaymentRequest request) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .policyId(request.getPolicyId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
