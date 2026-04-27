package com.isec.platform.modules.payments.repository.reactive;

import com.isec.platform.modules.payments.entity.Payment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReactivePaymentRepository extends ReactiveCrudRepository<Payment, UUID> {
}
