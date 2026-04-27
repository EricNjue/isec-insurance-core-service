package com.isec.platform.modules.payments.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.common.exception.PaymentNotFoundException;
import com.isec.platform.common.exception.PolicyNotFoundException;
import com.isec.platform.common.security.SecurityContextService;
import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.certificates.service.CertificateService;
import com.isec.platform.modules.customers.domain.Customer;
import com.isec.platform.modules.customers.service.CustomerService;
import com.isec.platform.modules.customers.dto.CustomerRequest;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PolicyService policyService;
    private final CertificateService certificateService;
    private final MpesaClient mpesaClient;
    private final MpesaRequestLogRepository mpesaRequestLogRepository;
    private final ApplicationRepository applicationRepository;
    private final CustomerService customerService;
    private final SecurityContextService securityContextService;
    private final ObjectMapper objectMapper;

    public Mono<Payment> initiateSTKPush(Long applicationId, BigDecimal amount, String phoneNumber) {
        return securityContextService.getCurrentUserId()
            .flatMap(userId -> {
                // Update/Create customer profile
                Mono<String> fullNameMono = securityContextService.getCurrentUserFullName().defaultIfEmpty("N/A");
                Mono<String> emailMono = securityContextService.getCurrentUserEmail().defaultIfEmpty("N/A");

                return Mono.zip(fullNameMono, emailMono)
                    .flatMap(tuple -> customerService.createOrUpdateCustomer(userId, CustomerRequest.builder()
                        .fullName(tuple.getT1())
                        .email(tuple.getT2())
                        .phoneNumber(phoneNumber)
                        .build()))
                    .thenReturn(userId);
            })
            .then(Mono.defer(() -> {
                log.info("Initiating STK Push for application: {}, amount: {}, phone: {}", applicationId, amount, phoneNumber);

                return policyService.getPolicyByApplicationId(applicationId)
                    .switchIfEmpty(Mono.error(new PolicyNotFoundException(applicationId)))
                    .flatMap(policy -> {
                        // Check threshold and balance
                        return paymentRepository.existsByApplicationIdAndStatus(applicationId, "COMPLETED")
                            .flatMap(exists -> {
                                if (!exists) {
                                    BigDecimal threshold = policy.getTotalAnnualPremium().multiply(new BigDecimal("0.35"));
                                    if (amount.compareTo(threshold) < 0) {
                                        log.warn("First payment threshold not met for application {}: amount {} is less than 35% of premium {}", 
                                                applicationId, amount, policy.getTotalAnnualPremium());
                                        return Mono.error(new BusinessException("The first payment must be at least 35% of the total annual premium (KES " + threshold + ")"));
                                    }
                                }

                                if (policy.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
                                    log.warn("Payment initiation skipped for application {}: Policy balance is already zero", applicationId);
                                    return Mono.error(new IllegalStateException("The policy for this application is already fully paid."));
                                }

                                return mpesaClient.initiateStkPush(phoneNumber, amount, "APP-" + applicationId)
                                    .flatMap(response -> {
                                        if (!"0".equals(response.responseCode())) {
                                            log.error("STK Push initiation failed: {}", response.responseDescription());
                                            return Mono.error(new RuntimeException("M-PESA STK Push failed: " + response.responseDescription()));
                                        }

                                        Payment payment = Payment.builder()
                                                .applicationId(applicationId)
                                                .amount(amount)
                                                .phoneNumber(phoneNumber)
                                                .status("PENDING")
                                                .checkoutRequestId(response.checkoutRequestId())
                                                .createdAt(LocalDateTime.now())
                                                .build();

                                        return paymentRepository.save(payment)
                                            .doOnNext(saved -> log.info("Payment record created with ID: {} and status: PENDING and checkoutRequestId: {}", saved.getId(), response.checkoutRequestId()));
                                    });
                            });
                    });
            }));
    }

    public Mono<Void> handleCallback(MpesaCallbackRequest request) {
        MpesaCallbackRequest.StkCallback callback = request.getBody().getStkCallback();
        String checkoutRequestId = callback.getCheckoutRequestId();

        log.info("Processing callback for checkoutRequestId: {}", checkoutRequestId);
        
        return saveCallbackLog(request)
            .then(paymentRepository.findByCheckoutRequestId(checkoutRequestId)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException(checkoutRequestId)))
                .flatMap(payment -> {
                    if (payment.getStatus().equals("COMPLETED")) {
                        log.info("Payment already processed for checkoutRequestId: {}", checkoutRequestId);
                        return Mono.empty();
                    }

                    if (callback.getResultCode() == 0) {
                        log.info("Payment successful for checkoutRequestId: {}", checkoutRequestId);

                        String receiptNumber = null;
                        if (callback.getCallbackMetadata() != null) {
                            for (MpesaCallbackRequest.Item item : callback.getCallbackMetadata().getItem()) {
                                if ("MpesaReceiptNumber".equals(item.getName())) {
                                    receiptNumber = (String) item.getValue();
                                    break;
                                }
                            }
                        }

                        final String finalReceiptNumber = receiptNumber;
                        return paymentRepository.existsByMpesaReceiptNumberAndIdNot(finalReceiptNumber, payment.getId())
                            .flatMap(exists -> {
                                if (finalReceiptNumber != null && exists) {
                                    log.error("Terminal error: duplicate M-Pesa receipt {} for payment id {}. Stopping further processing.", finalReceiptNumber, payment.getId());
                                    payment.setStatus("FAILED");
                                    return paymentRepository.save(payment).then();
                                }

                                payment.setStatus("COMPLETED");
                                if (finalReceiptNumber != null) {
                                    payment.setMpesaReceiptNumber(finalReceiptNumber);
                                }

                                return paymentRepository.save(payment)
                                    .flatMap(savedPayment -> policyService.updateBalance(savedPayment.getApplicationId(), savedPayment.getAmount())
                                        .flatMap(policy -> {
                                            log.info("Policy {} balance updated to {}. Triggering certificate issuance logic.", policy.getPolicyNumber(), policy.getBalance());

                                            return applicationRepository.findById(savedPayment.getApplicationId())
                                                .flatMap(app -> customerService.getCustomerByUserId(app.getUserId())
                                                    .map(customer -> {
                                                        return new CustomerInfo(customer.getEmail(), customer.getPhoneNumber());
                                                    })
                                                    .defaultIfEmpty(new CustomerInfo(null, savedPayment.getPhoneNumber()))
                                                )
                                                .defaultIfEmpty(new CustomerInfo(null, savedPayment.getPhoneNumber()))
                                                .flatMap(ci -> certificateService.processCertificateIssuance(policy, savedPayment.getAmount(), ci.email(), ci.phoneNumber()));
                                        })
                                    );
                            });
                    } else {
                        log.warn("Payment failed for checkoutRequestId: {}. Reason: {}", checkoutRequestId, callback.getResultDesc());
                        payment.setStatus("FAILED");
                        return paymentRepository.save(payment).then();
                    }
                }));
    }

    private Mono<Void> saveCallbackLog(MpesaCallbackRequest request) {
        try {
            MpesaCallbackRequest.StkCallback callback = request.getBody().getStkCallback();
            MpesaRequestLog logEntry = MpesaRequestLog.builder()
                    .requestType("CALLBACK")
                    .requestPayload(objectMapper.writeValueAsString(request))
                    .checkoutRequestId(callback.getCheckoutRequestId())
                    .merchantRequestId(callback.getMerchantRequestId())
                    .responseCode(String.valueOf(callback.getResultCode()))
                    .createdAt(LocalDateTime.now())
                    .build();
            return mpesaRequestLogRepository.save(logEntry).then();
        } catch (Exception e) {
            log.error("Failed to save M-PESA callback log", e);
            return Mono.empty();
        }
    }

    private record CustomerInfo(String email, String phoneNumber) {}
}
