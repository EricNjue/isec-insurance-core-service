package com.isec.platform.modules.applications.controller.motor;

import com.isec.platform.modules.applications.domain.motor.PaymentMethod;
import com.isec.platform.modules.applications.dto.motor.CalculateMotorPremiumRequest;
import com.isec.platform.modules.applications.dto.motor.MotorQuoteResponse;
import com.isec.platform.modules.applications.dto.motor.MpesaInitiationRequest;
import com.isec.platform.modules.applications.service.motor.MotorQuoteOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/motor/quotes")
@RequiredArgsConstructor
@Slf4j
public class MotorQuoteController {

    private final MotorQuoteOrchestrator orchestrator;

    @PostMapping("/calculate-premium")
    public Mono<ResponseEntity<MotorQuoteResponse>> calculatePremium(
            @Valid @RequestBody CalculateMotorPremiumRequest request
    ) {
        log.info("REST request to calculate motor premium for quoteId: {}", request.getQuoteId());
        return orchestrator.calculatePremium(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{quoteId}/accept")
    public Mono<ResponseEntity<MotorQuoteResponse>> acceptQuote(@PathVariable String quoteId) {
        log.info("REST request to accept motor quote: {}", quoteId);
        return orchestrator.acceptQuote(quoteId)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{quoteId}/payments/initiate")
    public Mono<ResponseEntity<MotorQuoteResponse>> initiateMpesaPayment(
            @PathVariable String quoteId,
            @RequestBody(required = false) MpesaInitiationRequest request
    ) {
        log.info("REST request to initiate M-Pesa payment for quote: {}", quoteId);
        MpesaInitiationRequest finalRequest = request != null ? request : new MpesaInitiationRequest();
        return orchestrator.initiatePayment(quoteId, finalRequest)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{quoteId}/payments/status")
    public Mono<ResponseEntity<MotorQuoteResponse>> getMpesaPaymentStatus(
            @PathVariable String quoteId,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) String receipt
    ) {
        log.info("REST request to check M-Pesa payment status for quote: {}, method: {}, receipt: {}", 
                quoteId, method, receipt);
        PaymentMethod finalMethod = method != null ? method : PaymentMethod.MPESA_STK;
        return orchestrator.checkPaymentStatus(quoteId, finalMethod, receipt)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{quoteId}")
    public Mono<ResponseEntity<MotorQuoteResponse>> getQuoteApplication(@PathVariable String quoteId) {
        log.info("REST request to get motor quote application: {}", quoteId);
        return orchestrator.getQuoteApplication(quoteId)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{quoteId}/issue-policy")
    public Mono<ResponseEntity<MotorQuoteResponse>> issuePolicy(@PathVariable String quoteId) {
        log.info("REST request to issue policy for quote: {}", quoteId);
        return orchestrator.issuePolicy(quoteId)
                .map(ResponseEntity::ok);
    }
}
