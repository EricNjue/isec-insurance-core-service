package com.isec.platform.modules.applications.service;

import com.isec.platform.common.exception.ResourceNotFoundException;
import com.isec.platform.common.security.SecurityContextService;
import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.domain.ApplicationStatus;
import com.isec.platform.modules.applications.dto.ApplicationRequest;
import com.isec.platform.modules.applications.dto.ApplicationResponse;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.customers.dto.CustomerRequest;
import com.isec.platform.modules.customers.service.CustomerService;
import com.isec.platform.modules.documents.service.ApplicationDocumentService;
import com.isec.platform.modules.policies.service.PolicyService;
import com.isec.platform.modules.rating.domain.AnonymousQuote;
import com.isec.platform.modules.rating.service.RatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentService documentService;
    private final RatingService ratingService;
    private final PolicyService policyService;
    private final CustomerService customerService;
    private final SecurityContextService securityContextService;

    @Transactional
    public ApplicationResponse createApplication(ApplicationRequest request) {
        String userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));

        log.info("Creating application for user: {} with reg number: {}", userId, request.getRegistrationNumber());

        // Update/Create customer profile from current JWT and request phone number
        customerService.createOrUpdateCustomer(userId, CustomerRequest.builder()
                .fullName(securityContextService.getCurrentUserFullName().orElse("N/A"))
                .email(securityContextService.getCurrentUserEmail().orElse("N/A"))
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : "N/A")
                .build());

        Application.ApplicationBuilder applicationBuilder = Application.builder()
                .userId(userId)
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
                applicationBuilder.vehicleMake(quote.getVehicleMake())
                        .vehicleModel(quote.getVehicleModel())
                        .yearOfManufacture(quote.getYearOfManufacture())
                        .vehicleValue(quote.getVehicleValue());
            }
        }

        Application saved = applicationRepository.save(applicationBuilder.build());
        log.info("Application created successfully with ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(Long id) {
        log.debug("Fetching application with ID: {}", id);
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", id));
        return mapToResponse(app);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listApplications() {
        boolean isAdmin = securityContextService.getCurrentJwt()
                .map(jwt -> {
                    if (jwt.getClaimAsMap("realm_access") != null) {
                        List<String> roles = (List<String>) jwt.getClaimAsMap("realm_access").get("roles");
                        return roles != null && roles.contains("ADMIN");
                    }
                    return false;
                }).orElse(false);

        String userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));

        List<Application> apps = isAdmin ? applicationRepository.findAll() : applicationRepository.findByUserId(userId);
        return apps.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public RatingService.PremiumBreakdown getQuote(Long applicationId, BigDecimal baseRate) {
        return applicationRepository.findById(applicationId)
                .map(app -> {
                    RatingService.PremiumBreakdown breakdown = ratingService.calculatePremium(app.getVehicleValue(), baseRate);

                    // Create Policy if it doesn't exist
                    policyService.createPolicy(app.getId(), breakdown.totalPremium());

                    // Update Application status
                    app.setStatus(ApplicationStatus.QUOTED);
                    applicationRepository.save(app);

                    return breakdown;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));
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
