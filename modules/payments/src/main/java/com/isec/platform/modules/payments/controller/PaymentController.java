package com.isec.platform.modules.payments.controller;

import com.isec.platform.modules.payments.domain.Payment;
import com.isec.platform.modules.payments.dto.StkPushRequest;
import com.isec.platform.modules.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/stk-push")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public ResponseEntity<Payment> initiateStkPush(@RequestBody StkPushRequest request) {
        Payment payment = paymentService.initiateSTKPush(
                request.getApplicationId(),
                request.getAmount(),
                request.getPhoneNumber());
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> mpesaCallback(@RequestBody String callbackPayload) {
        // Implementation of callback logic (idempotent)
        return ResponseEntity.ok(Map.of("ResultCode", "0", "ResultDesc", "Success"));
    }
}
