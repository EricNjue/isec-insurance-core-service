package com.isec.platform.modules.applications.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.isec.platform.modules.vehicles.dto.UserVehicleDto;
import com.isec.platform.modules.vehicles.service.UserVehicleService;
import com.isec.platform.modules.rating.dto.ReferralDecision;
import com.isec.platform.modules.rating.service.RatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentService documentService;
    private final RatingService ratingService;
    private final QuoteService quoteService;
    private final PolicyService policyService;
    private final CustomerService customerService;
    private final UserVehicleService userVehicleService;
    private final SecurityContextService securityContextService;
    private final ObjectMapper objectMapper;

    public Mono<ApplicationResponse> createApplication(ApplicationRequest request) {
        return securityContextService.getCurrentUserId()
                .switchIfEmpty(Mono.error(new IllegalStateException("User not authenticated")))
                .flatMap(userId -> {
                    log.info("Creating application for user: {} with reg number: {}", userId, request.getRegistrationNumber());

                    return Mono.zip(
                            securityContextService.getCurrentUserFullName().defaultIfEmpty("N/A"),
                            securityContextService.getCurrentUserEmail().defaultIfEmpty("N/A")
                    ).flatMap(tuple -> customerService.createOrUpdateCustomer(userId, CustomerRequest.builder()
                            .fullName(tuple.getT1())
                            .email(tuple.getT2())
                            .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : "N/A")
                            .build())
                    ).then(processApplicationCreation(userId, request));
                });
    }

    private Mono<ApplicationResponse> processApplicationCreation(String userId, ApplicationRequest request) {
        Application.ApplicationBuilder applicationBuilder = Application.builder()
                .userId(userId)
                .registrationNumber(request.getRegistrationNumber())
                .vehicleMake(request.getVehicleMake())
                .vehicleModel(request.getVehicleModel())
                .yearOfManufacture(request.getYearOfManufacture())
                .vehicleValue(request.getVehicleValue())
                .chassisNumber(request.getChassisNumber())
                .engineNumber(request.getEngineNumber())
                .status(ApplicationStatus.DRAFT);

        Mono<Void> enrichFromAnonymousQuote = (request.getAnonymousQuoteId() != null)
                ? ratingService.getAnonymousQuote(request.getAnonymousQuoteId())
                    .doOnNext(quote -> applicationBuilder.vehicleMake(quote.getVehicleMake())
                            .vehicleModel(quote.getVehicleModel())
                            .yearOfManufacture(quote.getYearOfManufacture())
                            .vehicleValue(quote.getVehicleValue()))
                    .then()
                : Mono.empty();

        Mono<Void> enrichFromOfficialQuote = (request.getQuoteId() != null)
                ? quoteService.getQuote(request.getQuoteId())
                    .doOnNext(quote -> {
                        applicationBuilder.vehicleMake(quote.getVehicleMake())
                                .vehicleModel(quote.getVehicleModel())
                                .yearOfManufacture(quote.getYearOfManufacture())
                                .vehicleValue(quote.getVehicleValue())
                                .registrationNumber(quote.getRegistrationNumber() != null ? quote.getRegistrationNumber() : request.getRegistrationNumber())
                                .chassisNumber(quote.getChassisNumber())
                                .engineNumber(quote.getEngineNumber())
                                .quoteId(quote.getQuoteId())
                                .rateBookId(quote.getRateBookId());

                        try {
                            applicationBuilder.pricingSnapshot(objectMapper.writeValueAsString(quote.getPricing()));
                        } catch (JsonProcessingException e) {
                            log.error("Failed to serialize pricing snapshot", e);
                        }

                        if (quote.getPricing().getReferralDecision() == ReferralDecision.REFERRED) {
                            applicationBuilder.status(ApplicationStatus.UNDERWRITING_REVIEW)
                                    .referralReason(quote.getPricing().getReferralReason());
                        } else {
                            applicationBuilder.status(ApplicationStatus.APPROVED_PENDING_PAYMENT);
                        }
                    })
                    .then()
                : Mono.empty();

        return Mono.when(enrichFromAnonymousQuote, enrichFromOfficialQuote)
                .then(Mono.defer(() -> applicationRepository.save(applicationBuilder.build())))
                .flatMap(saved -> {
                    Mono<Void> linkDocs = documentService.linkDocumentsToApplication(saved.getId(), request.getDocuments());

                    Mono<Void> createPolicy = (saved.getStatus() == ApplicationStatus.APPROVED_PENDING_PAYMENT && request.getQuoteId() != null)
                            ? quoteService.getQuote(request.getQuoteId())
                                .flatMap(quote -> policyService.createPolicy(saved.getId(), quote.getPricing().getTotalPremium()))
                                .then()
                            : Mono.empty();

                    Mono<Void> saveVehicle = userVehicleService.saveOrUpdateVehicle(userId, UserVehicleDto.builder()
                            .registrationNumber(saved.getRegistrationNumber())
                            .vehicleMake(saved.getVehicleMake())
                            .vehicleModel(saved.getVehicleModel())
                            .yearOfManufacture(saved.getYearOfManufacture())
                            .vehicleValue(saved.getVehicleValue())
                            .chassisNumber(saved.getChassisNumber())
                            .engineNumber(saved.getEngineNumber())
                            .build()).then();

                    return Mono.when(linkDocs, createPolicy, saveVehicle)
                            .then(mapToResponse(saved));
                });
    }

    public Mono<ApplicationResponse> getApplication(Long id) {
        log.debug("Fetching application with ID: {}", id);
        return applicationRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Application", id)))
                .flatMap(this::mapToResponse);
    }

    public Flux<ApplicationResponse> listApplications() {
        return Mono.zip(
                securityContextService.isAdmin(),
                securityContextService.getCurrentUserId(),
                com.isec.platform.common.multitenancy.TenantContext.getTenantId()
        ).flatMapMany(tuple -> {
                    boolean isAdmin = tuple.getT1();
                    String userId = tuple.getT2();
                    String tenantId = tuple.getT3();

                    return isAdmin
                            ? applicationRepository.findAllByTenantId(tenantId)
                            : applicationRepository.findByUserIdAndTenantId(userId, tenantId);
                })
                .flatMap(this::mapToResponse);
    }

    public Mono<RatingService.PremiumBreakdown> getQuote(Long applicationId, BigDecimal baseRate) {
        return applicationRepository.findById(applicationId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Application", applicationId)))
                .flatMap(app -> {
                    RatingService.PremiumBreakdown breakdown = ratingService.calculatePremium(app.getVehicleValue(), baseRate);

                    app.setStatus(ApplicationStatus.QUOTED);
                    return policyService.createPolicy(app.getId(), breakdown.totalPremium())
                            .flatMap(policy -> applicationRepository.save(app))
                            .thenReturn(breakdown);
                });
    }

    private Mono<ApplicationResponse> mapToResponse(Application app) {
        return documentService.getOrCreatePresignedUrls(app.getId())
                .map(docs -> ApplicationResponse.builder()
                        .id(app.getId())
                        .userId(app.getUserId())
                        .registrationNumber(app.getRegistrationNumber())
                        .vehicleMake(app.getVehicleMake())
                        .vehicleModel(app.getVehicleModel())
                        .yearOfManufacture(app.getYearOfManufacture())
                        .vehicleValue(app.getVehicleValue())
                        .status(app.getStatus())
                        .chassisNumber(app.getChassisNumber())
                        .engineNumber(app.getEngineNumber())
                        .createdAt(app.getCreatedAt())
                        .documents(docs)
                        .build());
    }
}
