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
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/motor/quotes")
@RequiredArgsConstructor
@Slf4j
public class QuoteController {

    private final QuoteService quoteService;

    @PostMapping("/initiate")
    @PreAuthorize("permitAll()")
    public Mono<ResponseEntity<InitiateQuoteResponse>> initiateQuote(
            @Valid @RequestBody InitiateQuoteRequest request) {
        return TenantContext.getTenantId()
                .flatMap(tenantId -> {
                    log.info("Initiate quote request received for tenant: {}, LPN: {}", 
                            tenantId, request.getLicensePlateNumber());
                    
                    return quoteService.initiateQuote(request)
                            .map(response -> {
                                if (response.getDoubleInsuranceCheck() != null && response.getDoubleInsuranceCheck().isHasDuplicate()) {
                                    return ResponseEntity.status(409).body(response);
                                }
                                return ResponseEntity.ok(response);
                            });
                });
    }

    @GetMapping("/initiate/{quoteId}")
    @PreAuthorize("permitAll()")
    public Mono<ResponseEntity<InitiateQuoteResponse>> getInitiatedQuote(
            @PathVariable String quoteId) {
        return TenantContext.getTenantId()
                .flatMap(tenantId -> {
                    log.info("Fetching initiated quote for tenant: {}, quoteId: {}", tenantId, quoteId);
                    return quoteService.getInitiatedQuote(quoteId)
                            .map(ResponseEntity::ok)
                            .defaultIfEmpty(ResponseEntity.notFound().build());
                });
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public Mono<ResponseEntity<QuoteResponse>> createQuote(
            @Valid @RequestBody QuoteRequest request) {
        return TenantContext.getTenantId()
                .flatMap(tenantId -> {
                    log.info("Create quote request received for tenant: {}, quoteId: {}", tenantId, request.getQuoteId());
                    return quoteService.calculateQuote(request)
                            .map(ResponseEntity::ok);
                });
    }
}
