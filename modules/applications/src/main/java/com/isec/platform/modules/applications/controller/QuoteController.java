package com.isec.platform.modules.applications.controller;

import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.applications.dto.InitiateQuoteRequest;
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
@RequestMapping("/api/v1/motor/quotes")
@RequiredArgsConstructor
@Slf4j
public class QuoteController {

    private final QuoteService quoteService;

    @PostMapping("/initiate")
    @PreAuthorize("permitAll()")
    public ResponseEntity<InitiateQuoteResponse> initiateQuote(
            @Valid @RequestBody InitiateQuoteRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Initiate quote request received for tenant: {}, LPN: {}", 
                tenantId, request.getLicensePlateNumber());
        return ResponseEntity.ok(quoteService.initiateQuote(request));
    }

    @GetMapping("/initiate/{quoteId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<InitiateQuoteResponse> getInitiatedQuote(
            @PathVariable String quoteId) {
        String tenantId = TenantContext.getTenantId();
        log.info("Fetching initiated quote for tenant: {}, quoteId: {}", tenantId, quoteId);
        InitiateQuoteResponse response = quoteService.getInitiatedQuote(quoteId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public ResponseEntity<QuoteResponse> createQuote(
            @Valid @RequestBody QuoteRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Create quote request received for tenant: {}, quoteId: {}", tenantId, request.getQuoteId());
        return ResponseEntity.ok(quoteService.calculateQuote(request));
    }
}
