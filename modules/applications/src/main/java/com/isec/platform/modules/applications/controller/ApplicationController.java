package com.isec.platform.modules.applications.controller;

import com.isec.platform.modules.applications.dto.ApplicationRequest;
import com.isec.platform.modules.applications.dto.ApplicationResponse;
import com.isec.platform.modules.applications.service.ApplicationService;
import com.isec.platform.modules.rating.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody ApplicationRequest request) {
        log.info("Create application request received");
        return ResponseEntity.ok(applicationService.createApplication(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<ApplicationResponse> getApplication(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getApplication(id));
    }

    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> listApplications() {
        return ResponseEntity.ok(applicationService.listApplications());
    }

    @PostMapping("/quote")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<RatingService.PremiumBreakdown> getQuote(
            @RequestParam Long applicationId,
            @RequestParam java.math.BigDecimal baseRate) {
        return ResponseEntity.ok(applicationService.getQuote(applicationId, baseRate));
    }

}
