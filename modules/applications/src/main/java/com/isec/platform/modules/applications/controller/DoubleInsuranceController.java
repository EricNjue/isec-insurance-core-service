package com.isec.platform.modules.applications.controller;

import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.applications.dto.DoubleInsuranceRequest;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckResponse;
import com.isec.platform.modules.applications.service.QuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insurance-check")
@RequiredArgsConstructor
@Slf4j
public class DoubleInsuranceController {

    private final QuoteService quoteService;

    @PostMapping("/double-insurance")
    @PreAuthorize("permitAll()")
    public ResponseEntity<DoubleInsuranceCheckResponse> checkDoubleInsurance(
            @Valid @RequestBody DoubleInsuranceRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Double insurance check request received for tenant: {}, registration: {}", 
                tenantId, request.getLicensePlateNumber());
        
        DoubleInsuranceCheckResponse response = quoteService.checkDoubleInsurance(
                request.getLicensePlateNumber(), request.getChassisNumber());
        
        return ResponseEntity.ok(response);
    }
}
