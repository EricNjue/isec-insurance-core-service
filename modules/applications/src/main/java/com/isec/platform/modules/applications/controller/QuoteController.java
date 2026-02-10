package com.isec.platform.modules.applications.controller;

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

    @PostMapping
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public ResponseEntity<QuoteResponse> createQuote(
            @PathVariable String tenantId,
            @Valid @RequestBody QuoteRequest request) {
        log.info("Create quote request received for tenant: {}", tenantId);
        // tenantId from path is handled by TenantFilter if passed in header,
        // but path variable is explicitly requested in API spec.
        return ResponseEntity.ok(quoteService.calculateQuote(request));
    }
}
