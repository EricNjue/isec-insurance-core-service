package com.isec.platform.modules.payments.service;

import com.isec.platform.modules.payments.domain.Payment;
import com.isec.platform.modules.payments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private static final int MAX_PARTIAL_PAYMENTS = 4;

    @Transactional
    public Payment initiateSTKPush(Long applicationId, BigDecimal amount, String phoneNumber) {
        List<Payment> existingPayments = paymentRepository.findByApplicationId(applicationId);
        if (existingPayments.size() >= MAX_PARTIAL_PAYMENTS) {
            throw new IllegalStateException("Maximum partial payments (4) reached for this application.");
        }

        Payment payment = Payment.builder()
                .applicationId(applicationId)
                .amount(amount)
                .phoneNumber(phoneNumber)
                .status("PENDING")
                .build();

        return paymentRepository.save(payment);
    }
}
