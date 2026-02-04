package com.isec.platform.modules.payments.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.modules.certificates.service.CertificateService;
import com.isec.platform.modules.integrations.mpesa.MpesaClient;
import com.isec.platform.modules.integrations.mpesa.domain.MpesaRequestLog;
import com.isec.platform.modules.integrations.mpesa.repository.MpesaRequestLogRepository;
import com.isec.platform.modules.payments.domain.Payment;
import com.isec.platform.modules.payments.dto.MpesaCallbackRequest;
import com.isec.platform.modules.payments.repository.PaymentRepository;
import com.isec.platform.modules.policies.domain.Policy;
import com.isec.platform.modules.policies.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PolicyService policyService;
    private final CertificateService certificateService;
    private final MpesaClient mpesaClient;
    private final MpesaRequestLogRepository mpesaRequestLogRepository;
    private final ObjectMapper objectMapper;
    private static final int MAX_PARTIAL_PAYMENTS = 4;

    @Transactional
    public Payment initiateSTKPush(Long applicationId, BigDecimal amount, String phoneNumber) {
        log.info("Initiating STK Push for application: {}, amount: {}, phone: {}", applicationId, amount, phoneNumber);

        List<Payment> existingPayments = paymentRepository.findByApplicationId(applicationId);
        if (existingPayments.size() >= MAX_PARTIAL_PAYMENTS) {
            log.error("Payment initiation failed for application {}: Max payments reached", applicationId);
            throw new IllegalStateException("Maximum partial payments (4) reached for this application.");
        }

        MpesaClient.MpesaResponse response = mpesaClient.initiateStkPush(phoneNumber, amount, "APP-" + applicationId);
        
        if (!"0".equals(response.responseCode())) {
            log.error("STK Push initiation failed: {}", response.responseDescription());
            throw new RuntimeException("M-PESA STK Push failed: " + response.responseDescription());
        }

        Payment payment = Payment.builder()
                .applicationId(applicationId)
                .amount(amount)
                .phoneNumber(phoneNumber)
                .status("PENDING")
                .checkoutRequestId(response.checkoutRequestId())
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment record created with ID: {} and status: PENDING and checkoutRequestId: {}", saved.getId(), response.checkoutRequestId());
        return saved;
    }

    @Transactional
    public void handleCallback(MpesaCallbackRequest request) {
        MpesaCallbackRequest.StkCallback callback = request.getBody().getStkCallback();
        String checkoutRequestId = callback.getCheckoutRequestId();

        log.info("Processing callback for checkoutRequestId: {}", checkoutRequestId);
        saveCallbackLog(request);

        Payment payment = paymentRepository.findByCheckoutRequestId(checkoutRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for checkoutRequestId: " + checkoutRequestId));

        if (payment.getStatus().equals("COMPLETED")) {
            log.info("Payment already processed for checkoutRequestId: {}", checkoutRequestId);
            return;
        }

        if (callback.getResultCode() == 0) {
            log.info("Payment successful for checkoutRequestId: {}", checkoutRequestId);

            // Extract Mpesa receipt before persisting to detect duplicates deterministically
            String receiptNumber = null;
            if (callback.getCallbackMetadata() != null) {
                for (MpesaCallbackRequest.Item item : callback.getCallbackMetadata().getItem()) {
                    if ("MpesaReceiptNumber".equals(item.getName())) {
                        receiptNumber = (String) item.getValue();
                        break;
                    }
                }
            }

            // Guard: if the same receipt is already recorded on another payment, treat as terminal and stop
            if (receiptNumber != null && paymentRepository.existsByMpesaReceiptNumberAndIdNot(receiptNumber, payment.getId())) {
                log.error("Terminal error: duplicate M-Pesa receipt {} for payment id {}. Stopping further processing.", receiptNumber, payment.getId());
                payment.setStatus("FAILED");
                // Do NOT set the duplicate receipt on this record to avoid constraint violations
                paymentRepository.save(payment);
                paymentRepository.flush();
                return; // Stop: no policy update, no certificate issuance
            }

            payment.setStatus("COMPLETED");
            if (receiptNumber != null) {
                payment.setMpesaReceiptNumber(receiptNumber);
            }

            try {
                paymentRepository.save(payment);
                // Force flush so unique constraints are validated before proceeding to side effects
                paymentRepository.flush();
            } catch (DataIntegrityViolationException ex) {
                log.error("Terminal DB error while saving payment {}: {}. Stopping further processing.", payment.getId(), ex.getMessage());
                // Propagate or just stop silently; choose to stop without side-effects
                return;
            }

            // Update Policy balance and trigger certificate issuance
            Policy policy = policyService.updateBalance(payment.getApplicationId(), payment.getAmount());

            log.info("Policy {} balance updated to {}. Triggering certificate issuance logic.", policy.getPolicyNumber(), policy.getBalance());

            // Trigger Certificate Issuance Logic
            certificateService.processCertificateIssuance(policy, payment.getAmount());

        } else {
            log.warn("Payment failed for checkoutRequestId: {}. Reason: {}", checkoutRequestId, callback.getResultDesc());
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
        }
    }

    private void saveCallbackLog(MpesaCallbackRequest request) {
        try {
            MpesaCallbackRequest.StkCallback callback = request.getBody().getStkCallback();
            MpesaRequestLog logEntry = MpesaRequestLog.builder()
                    .requestType("CALLBACK")
                    .requestPayload(objectMapper.writeValueAsString(request))
                    .checkoutRequestId(callback.getCheckoutRequestId())
                    .merchantRequestId(callback.getMerchantRequestId())
                    .responseCode(String.valueOf(callback.getResultCode()))
                    .build();
            mpesaRequestLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to save M-PESA callback log", e);
        }
    }
}
