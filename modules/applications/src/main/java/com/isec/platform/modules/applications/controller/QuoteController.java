package com.isec.platform.modules.applications.controller;

import com.isec.platform.modules.applications.dto.InitiateQuoteResponse;
import com.isec.platform.modules.applications.dto.QuoteRequest;
import com.isec.platform.modules.applications.dto.QuoteResponse;
import com.isec.platform.modules.applications.service.QuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/{tenantId}/motor/quotes")
@RequiredArgsConstructor
@Slf4j
public class QuoteController {

    private final QuoteService quoteService;

    @PostMapping("/initiate")
    @PreAuthorize("permitAll()")
    public ResponseEntity<InitiateQuoteResponse> initiateQuote(
            @PathVariable String tenantId) {
        log.info("Initiate quote request received for tenant: {}", tenantId);
        return ResponseEntity.ok(quoteService.initiateQuote());
    }

    @GetMapping("/initiate/{quoteId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<InitiateQuoteResponse> getInitiatedQuote(
            @PathVariable String tenantId,
            @PathVariable String quoteId) {
        log.info("Fetching initiated quote for tenant: {}, quoteId: {}", tenantId, quoteId);
        InitiateQuoteResponse response = quoteService.getInitiatedQuote(quoteId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public ResponseEntity<QuoteResponse> createQuote(
            @PathVariable String tenantId,
            @Valid @RequestBody QuoteRequest request) {
        log.info("Create quote request received for tenant: {}, quoteId: {}", tenantId, request.getQuoteId());
        return ResponseEntity.ok(quoteService.calculateQuote(request));
    }
}
