package com.isec.platform.modules.payments.repository;

import com.isec.platform.modules.payments.domain.Payment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentRepository extends ReactiveCrudRepository<Payment, Long> {
    Flux<Payment> findByApplicationId(Long applicationId);
    Mono<Boolean> existsByApplicationIdAndStatus(Long applicationId, String status);
    Mono<Payment> findByCheckoutRequestId(String checkoutRequestId);
    Mono<Payment> findByMpesaReceiptNumber(String mpesaReceiptNumber);
    Mono<Boolean> existsByMpesaReceiptNumberAndIdNot(String mpesaReceiptNumber, Long id);
}
