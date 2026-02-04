package com.isec.platform.modules.payments.service;

import com.isec.platform.modules.payments.domain.Payment;
import com.isec.platform.modules.payments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private static final int MAX_PARTIAL_PAYMENTS = 4;

    @Transactional
    public Payment initiateSTKPush(Long applicationId, BigDecimal amount, String phoneNumber) {
        log.info("Initiating STK Push for application: {}, amount: {}, phone: {}", applicationId, amount, phoneNumber);
        
        List<Payment> existingPayments = paymentRepository.findByApplicationId(applicationId);
        if (existingPayments.size() >= MAX_PARTIAL_PAYMENTS) {
            log.error("Payment initiation failed for application {}: Max payments reached", applicationId);
            throw new IllegalStateException("Maximum partial payments (4) reached for this application.");
        }

        Payment payment = Payment.builder()
                .applicationId(applicationId)
                .amount(amount)
                .phoneNumber(phoneNumber)
                .status("PENDING")
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment record created with ID: {} and status: PENDING", saved.getId());
        return saved;
    }
}
