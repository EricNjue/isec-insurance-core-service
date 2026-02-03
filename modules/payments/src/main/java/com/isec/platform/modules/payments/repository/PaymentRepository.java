package com.isec.platform.modules.payments.repository;

import com.isec.platform.modules.payments.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByApplicationId(Long applicationId);
    Optional<Payment> findByCheckoutRequestId(String checkoutRequestId);
}
