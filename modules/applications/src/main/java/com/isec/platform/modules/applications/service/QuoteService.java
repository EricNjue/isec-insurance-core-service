package com.isec.platform.modules.applications.service;

import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.applications.dto.InitiateQuoteRequest;
import com.isec.platform.modules.applications.dto.InitiateQuoteResponse;
import com.isec.platform.modules.applications.dto.QuoteRequest;
import com.isec.platform.modules.applications.dto.QuoteResponse;
import com.isec.platform.modules.customers.dto.CustomerRequest;
import com.isec.platform.modules.customers.service.CustomerService;
import com.isec.platform.modules.documents.dto.ApplicationDocumentDto;
import com.isec.platform.modules.documents.service.ApplicationDocumentService;
import com.isec.platform.modules.integrations.common.adapter.InsuranceIntegrationAdapter;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckRequest;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckResponse;
import com.isec.platform.modules.vehicles.dto.UserVehicleDto;
import com.isec.platform.modules.vehicles.service.UserVehicleService;
import com.isec.platform.modules.rating.dto.PricingResult;
import com.isec.platform.modules.rating.dto.RatingContext;
import com.isec.platform.modules.rating.service.PricingEngine;
import com.isec.platform.modules.rating.service.RateBookSnapshotLoader;
import com.isec.platform.common.security.SecurityContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteService {

    private final PricingEngine pricingEngine;
    private final RateBookSnapshotLoader rateBookSnapshotLoader;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationDocumentService documentService;
    private final CustomerService customerService;
    private final UserVehicleService userVehicleService;
    private final SecurityContextService securityContextService;
    private final Map<String, InsuranceIntegrationAdapter> integrationAdapters;

    @Value("${quote.cache.duration-minutes:30}")
    private int quoteCacheDurationMinutes;

    public InitiateQuoteResponse initiateQuote(InitiateQuoteRequest request) {
        String quoteId = UUID.randomUUID().toString();
        String tenantId = getTenantIdOrThrow();

        log.info("Initiating quote with ID: {}, Tenant: {}, LPN: {}", 
                quoteId, tenantId, request.getLicensePlateNumber());

        // Resolve adapter and check double insurance
        DoubleInsuranceCheckResponse doubleInsuranceCheck = performDoubleInsuranceCheck(
                tenantId, request.getLicensePlateNumber(), request.getChassisNumber());

        if (doubleInsuranceCheck != null && doubleInsuranceCheck.isHasDuplicate()) {
            log.warn("Double insurance found for LPN: {}. Skipping document generation and caching.", 
                    request.getLicensePlateNumber());
            return InitiateQuoteResponse.builder()
                    .quoteId(quoteId)
                    .doubleInsuranceCheck(doubleInsuranceCheck)
                    .build();
        }

        // Required documents for application
        log.debug("Fetching required documents for quote initiation");
        List<ApplicationDocumentDto> documents = documentService.getOrCreatePresignedUrls(null); 

        InitiateQuoteResponse response = InitiateQuoteResponse.builder()
                .quoteId(quoteId)
                .documents(documents)
                .doubleInsuranceCheck(doubleInsuranceCheck)
                .build();

        // Cache quote initiation for 30 minutes
        log.info("Caching initiated quote {} for {} minutes", quoteId, quoteCacheDurationMinutes);
        redisTemplate.opsForValue().set("quote_init:" + quoteId, response, Duration.ofMinutes(quoteCacheDurationMinutes));

        return response;
    }

    public DoubleInsuranceCheckResponse checkDoubleInsurance(String registrationNumber, String chassisNumber) {
        String tenantId = getTenantIdOrThrow();
        log.info("Direct double insurance check for tenant: {}, registration: {}", tenantId, registrationNumber);
        return performDoubleInsuranceCheck(tenantId, registrationNumber, chassisNumber);
    }

    private String getTenantIdOrThrow() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            log.error("Tenant ID not found in context");
            throw new com.isec.platform.common.exception.BusinessException("Missing required X-Tenant-Id header");
        }
        return tenantId;
    }

    private DoubleInsuranceCheckResponse performDoubleInsuranceCheck(String tenantId, String registrationNumber, String chassisNumber) {
        InsuranceIntegrationAdapter adapter = integrationAdapters.get(tenantId.toUpperCase() + "IntegrationAdapter");
        if (adapter == null) {
            log.debug("Direct adapter lookup failed for {}. Trying case-insensitive search.", tenantId);
            adapter = integrationAdapters.values().stream()
                    .filter(a -> a.getCompanyCode().equalsIgnoreCase(tenantId))
                    .findFirst()
                    .orElse(null);
        }

        if (adapter != null) {
            log.info("Resolved adapter: {} for tenant: {}", adapter.getClass().getSimpleName(), tenantId);
            DoubleInsuranceCheckResponse response = adapter.checkDoubleInsurance(DoubleInsuranceCheckRequest.builder()
                    .registrationNumber(registrationNumber)
                    .chassisNumber(chassisNumber)
                    .build());
            log.info("Double insurance check result for {}: hasDuplicate={}", 
                    registrationNumber, response.isHasDuplicate());
            return response;
        } else {
            log.warn("No integration adapter found for tenant: {}. Skipping double insurance check.", tenantId);
            return DoubleInsuranceCheckResponse.builder()
                    .hasDuplicate(false)
                    .status("clear")
                    .message("No integration adapter configured for this tenant")
                    .build();
        }
    }

    public QuoteResponse calculateQuote(QuoteRequest request) {
        String tenantId = getTenantIdOrThrow();
        
        String quoteId = request.getQuoteId();
        
        log.info("Calculating quote for tenant: {}, quoteId: {}", tenantId, quoteId);

        if (quoteId != null && redisTemplate.opsForValue().get("quote_init:" + quoteId) == null) {
             log.warn("Quote ID {} not found in cache or expired", quoteId);
        }

        QuoteRequest.InsuranceDetails insurance = request.getInsuranceDetails();
        QuoteRequest.VehicleDetails vehicle = request.getVehicleDetails();
        QuoteRequest.KycDetails kyc = request.getKycDetails();

        int vehicleAge = LocalDateTime.now().getYear() - vehicle.getYearOfManufacture();

        RatingContext ratingContext = RatingContext.builder()
                .tenantId(tenantId)
                .category(insurance.getCategory())
                .vehicleValue(vehicle.getValuationAmount())
                .vehicleAge(vehicleAge)
                .vehicleMake(vehicle.getMakeCode())
                .vehicleModel(vehicle.getModelCode())
                .selectedAddonIds(insurance.getAddonRuleIds() != null ? new HashSet<>(insurance.getAddonRuleIds()) : new HashSet<>())
                .additionalData(insurance.getAdditionalData())
                .build();

        PricingResult pricingResult = pricingEngine.price(ratingContext);

        var snapshot = rateBookSnapshotLoader.loadActive(tenantId);
        Long rateBookId = snapshot != null ? snapshot.rateBookId() : null;
        String rateBookVersion = snapshot != null ? snapshot.version() : null;
        String cacheKey = snapshot != null ? snapshot.cacheKey() : null;

        QuoteResponse response = QuoteResponse.builder()
                .quoteId(quoteId != null ? quoteId : UUID.randomUUID().toString())
                .tenantId(tenantId)
                .category(insurance.getCategory())
                .vehicleMake(vehicle.getMakeCode())
                .vehicleModel(vehicle.getModelCode())
                .yearOfManufacture(vehicle.getYearOfManufacture())
                .vehicleValue(vehicle.getValuationAmount())
                .registrationNumber(vehicle.getLicensePlateNumber())
                .chassisNumber(vehicle.getChassisNumber())
                .engineNumber(vehicle.getEngineNumber())
                .rateBookId(rateBookId)
                .rateBookVersion(rateBookVersion)
                .cacheKey(cacheKey)
                .pricing(pricingResult)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .build();

        // Handle KYC details - Upsert Customer
        if (kyc != null) {
            securityContextService.getCurrentUserId().ifPresent(userId -> {
                log.info("Upserting customer details for user: {}", userId);
                customerService.createOrUpdateCustomer(userId, CustomerRequest.builder()
                        .fullName(kyc.getFullName())
                        .email(kyc.getEmail())
                        .phoneNumber(kyc.getPhoneNumber())
                        .physicalAddress(kyc.getPhysicalAddress())
                        .build());

                // Associate vehicle with customer
                log.info("Associating vehicle {} with user: {}", vehicle.getLicensePlateNumber(), userId);
                userVehicleService.saveOrUpdateVehicle(userId, UserVehicleDto.builder()
                        .registrationNumber(vehicle.getLicensePlateNumber())
                        .vehicleMake(vehicle.getMakeCode())
                        .vehicleModel(vehicle.getModelCode())
                        .yearOfManufacture(vehicle.getYearOfManufacture())
                        .vehicleValue(vehicle.getValuationAmount())
                        .chassisNumber(vehicle.getChassisNumber())
                        .engineNumber(vehicle.getEngineNumber())
                        .build());
            });
        }

        // Cache quote for application conversion
        redisTemplate.opsForValue().set("quote_v2:" + response.getQuoteId(), response, Duration.ofDays(30));

        return response;
    }

    public QuoteResponse getQuote(String quoteId) {
        return (QuoteResponse) redisTemplate.opsForValue().get("quote_v2:" + quoteId);
    }

    public InitiateQuoteResponse getInitiatedQuote(String quoteId) {
        return (InitiateQuoteResponse) redisTemplate.opsForValue().get("quote_init:" + quoteId);
    }
}
