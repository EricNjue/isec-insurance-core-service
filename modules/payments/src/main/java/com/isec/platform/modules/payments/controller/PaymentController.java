package com.isec.platform.modules.payments.controller;

import com.isec.platform.modules.payments.dto.MpesaCallbackRequest;
import com.isec.platform.modules.payments.domain.Payment;
import com.isec.platform.modules.payments.dto.StkPushRequest;
import com.isec.platform.modules.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/stk-push")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public ResponseEntity<Payment> initiateStkPush(@RequestBody StkPushRequest request) {
        log.info("STK Push initiation request received. applicationId={}, amount={}, phone=***{}", request.getApplicationId(), request.getAmount(),
                request.getPhoneNumber() != null && request.getPhoneNumber().length() >= 4 ? request.getPhoneNumber().substring(request.getPhoneNumber().length() - 4) : "");
        Payment payment = paymentService.initiateSTKPush(
                request.getApplicationId(),
                request.getAmount(),
                request.getPhoneNumber());
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> mpesaCallback(@RequestBody MpesaCallbackRequest request) {
        log.info("Received M-Pesa callback. checkoutRequestId={}, resultCode={}",
                request.getBody() != null && request.getBody().getStkCallback() != null ? request.getBody().getStkCallback().getCheckoutRequestId() : "N/A",
                request.getBody() != null && request.getBody().getStkCallback() != null ? request.getBody().getStkCallback().getResultCode() : null);
        paymentService.handleCallback(request);
        return ResponseEntity.ok(Map.of("ResultCode", "0", "ResultDesc", "Success"));
    }
}
