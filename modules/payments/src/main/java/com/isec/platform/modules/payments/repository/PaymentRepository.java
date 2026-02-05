package com.isec.platform.modules.payments.repository;

import com.isec.platform.modules.payments.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByApplicationId(Long applicationId);
    boolean existsByApplicationIdAndStatus(Long applicationId, String status);
    Optional<Payment> findByCheckoutRequestId(String checkoutRequestId);
    Optional<Payment> findByMpesaReceiptNumber(String mpesaReceiptNumber);
    boolean existsByMpesaReceiptNumberAndIdNot(String mpesaReceiptNumber, Long id);
}
