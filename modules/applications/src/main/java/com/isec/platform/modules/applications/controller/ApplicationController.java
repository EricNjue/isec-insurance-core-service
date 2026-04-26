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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public Mono<ResponseEntity<ApplicationResponse>> createApplication(
            @Valid @RequestBody ApplicationRequest request) {
        log.info("Create application request received");
        return applicationService.createApplication(request)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public Mono<ResponseEntity<ApplicationResponse>> getApplication(@PathVariable Long id) {
        return applicationService.getApplication(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    public Mono<ResponseEntity<List<ApplicationResponse>>> listApplications() {
        return applicationService.listApplications()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/quote")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public Mono<ResponseEntity<RatingService.PremiumBreakdown>> getQuote(
            @RequestParam Long applicationId,
            @RequestParam java.math.BigDecimal baseRate) {
        return applicationService.getQuote(applicationId, baseRate)
                .map(ResponseEntity::ok);
    }
}
