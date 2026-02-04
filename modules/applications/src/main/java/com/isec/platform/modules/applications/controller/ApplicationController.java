package com.isec.platform.modules.applications.controller;

import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.dto.ApplicationRequest;
import com.isec.platform.modules.applications.dto.ApplicationResponse;
import com.isec.platform.modules.applications.domain.ApplicationStatus;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.documents.service.ApplicationDocumentService;
import com.isec.platform.modules.rating.domain.AnonymousQuote;
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
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentService documentService;
    private final RatingService ratingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody ApplicationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        log.info("Creating application for user: {} with reg number: {}", jwt.getSubject(), request.getRegistrationNumber());
        
        Application.ApplicationBuilder applicationBuilder = Application.builder()
                .userId(jwt.getSubject())
                .registrationNumber(request.getRegistrationNumber())
                .vehicleMake(request.getVehicleMake())
                .vehicleModel(request.getVehicleModel())
                .yearOfManufacture(request.getYearOfManufacture())
                .vehicleValue(request.getVehicleValue())
                .status(ApplicationStatus.DRAFT);

        if (request.getAnonymousQuoteId() != null) {
            log.info("Proceeding from anonymous quote ID: {}", request.getAnonymousQuoteId());
            Optional<AnonymousQuote> anonymousQuote = ratingService.getAnonymousQuote(request.getAnonymousQuoteId());
            if (anonymousQuote.isPresent()) {
                AnonymousQuote quote = anonymousQuote.get();
                // We can override details from the quote if they were missing or mismatched, 
                // but here we just associate/log it. 
                // In a real system, you might want to lock the premium or store the quote ID in the application.
                applicationBuilder.vehicleMake(quote.getVehicleMake())
                        .vehicleModel(quote.getVehicleModel())
                        .yearOfManufacture(quote.getYearOfManufacture())
                        .vehicleValue(quote.getVehicleValue());
            }
        }

        Application saved = applicationRepository.save(applicationBuilder.build());
        log.info("Application created successfully with ID: {}", saved.getId());
        return ResponseEntity.ok(mapToResponse(saved));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<ApplicationResponse> getApplication(@PathVariable Long id) {
        log.debug("Fetching application with ID: {}", id);
        return applicationRepository.findById(id)
                .map(app -> ResponseEntity.ok(mapToResponse(app)))
                .orElseGet(() -> {
                    log.warn("Application not found with ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> listApplications(@AuthenticationPrincipal Jwt jwt) {
        List<Application> apps;
        boolean isAdmin = false;
        if (jwt.getClaimAsMap("realm_access") != null) {
            List<String> roles = (List<String>) jwt.getClaimAsMap("realm_access").get("roles");
            if (roles != null && roles.contains("ADMIN")) {
                isAdmin = true;
            }
        }

        if (isAdmin) {
            apps = applicationRepository.findAll();
        } else {
            apps = applicationRepository.findByUserId(jwt.getSubject());
        }
        return ResponseEntity.ok(apps.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @PostMapping("/quote")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<RatingService.PremiumBreakdown> getQuote(
            @RequestParam Long applicationId,
            @RequestParam java.math.BigDecimal baseRate) {
        return applicationRepository.findById(applicationId)
                .map(app -> ResponseEntity.ok(ratingService.calculatePremium(app.getVehicleValue(), baseRate)))
                .orElse(ResponseEntity.notFound().build());
    }

    private ApplicationResponse mapToResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .userId(app.getUserId())
                .registrationNumber(app.getRegistrationNumber())
                .vehicleMake(app.getVehicleMake())
                .vehicleModel(app.getVehicleModel())
                .yearOfManufacture(app.getYearOfManufacture())
                .vehicleValue(app.getVehicleValue())
                .status(app.getStatus())
                .createdAt(app.getCreatedAt())
                .documents(documentService.getOrCreatePresignedUrls(app.getId()))
                .build();
    }
}
