package com.isec.platform.modules.applications.service;

import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.applications.dto.InitiateQuoteRequest;
import com.isec.platform.modules.applications.dto.InitiateQuoteResponse;
import com.isec.platform.modules.applications.dto.QuoteRequest;
import com.isec.platform.modules.applications.dto.QuoteResponse;
import com.isec.platform.modules.customers.dto.CustomerRequest;
import com.isec.platform.modules.customers.service.CustomerService;
import com.isec.platform.modules.documents.service.ApplicationDocumentService;
import com.isec.platform.modules.integrations.common.adapter.InsuranceIntegrationAdapter;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckRequest;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckResponse;
import com.isec.platform.modules.vehicles.dto.UserVehicleDto;
import com.isec.platform.modules.vehicles.service.UserVehicleService;
import com.isec.platform.modules.rating.dto.RatingContext;
import com.isec.platform.modules.rating.service.PricingEngine;
import com.isec.platform.modules.rating.service.RateBookSnapshotLoader;
import com.isec.platform.common.security.SecurityContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteService {

    private final PricingEngine pricingEngine;
    private final RateBookSnapshotLoader rateBookSnapshotLoader;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ApplicationDocumentService documentService;
    private final CustomerService customerService;
    private final UserVehicleService userVehicleService;
    private final SecurityContextService securityContextService;
    private final Map<String, InsuranceIntegrationAdapter> integrationAdapters;

    @Value("${quote.cache.duration-minutes:30}")
    private int quoteCacheDurationMinutes;

    public Mono<InitiateQuoteResponse> initiateQuote(InitiateQuoteRequest request) {
        String quoteId = UUID.randomUUID().toString();
        
        return getTenantIdOrThrow()
            .flatMap(tenantId -> {
                log.info("Initiating quote with ID: {}, Tenant: {}, LPN: {}", 
                        quoteId, tenantId, request.getLicensePlateNumber());

                return performDoubleInsuranceCheck(tenantId, request.getLicensePlateNumber(), request.getChassisNumber())
                    .flatMap(doubleInsuranceCheck -> {
                        if (doubleInsuranceCheck != null && doubleInsuranceCheck.isHasDuplicate()) {
                            log.warn("Double insurance found for LPN: {}. Skipping document generation and caching.", 
                                    request.getLicensePlateNumber());
                            return Mono.just(InitiateQuoteResponse.builder()
                                    .quoteId(quoteId)
                                    .doubleInsuranceCheck(doubleInsuranceCheck)
                                    .build());
                        }

                        log.debug("Fetching required documents for quote initiation");
                        return documentService.getOrCreatePresignedUrls(null)
                            .flatMap(documents -> {
                                InitiateQuoteResponse response = InitiateQuoteResponse.builder()
                                        .quoteId(quoteId)
                                        .documents(documents)
                                        .doubleInsuranceCheck(doubleInsuranceCheck)
                                        .build();

                                log.info("Caching initiated quote {} for {} minutes", quoteId, quoteCacheDurationMinutes);
                                return redisTemplate.opsForValue()
                                        .set("quote_init:" + quoteId, response, Duration.ofMinutes(quoteCacheDurationMinutes))
                                        .thenReturn(response);
                            });
                    });
            });
    }

    public Mono<DoubleInsuranceCheckResponse> checkDoubleInsurance(String registrationNumber, String chassisNumber) {
        return getTenantIdOrThrow()
            .flatMap(tenantId -> {
                log.info("Direct double insurance check for tenant: {}, registration: {}", tenantId, registrationNumber);
                return performDoubleInsuranceCheck(tenantId, registrationNumber, chassisNumber);
            });
    }

    private Mono<String> getTenantIdOrThrow() {
        return TenantContext.getTenantId()
            .switchIfEmpty(Mono.error(new BusinessException("Missing required X-Tenant-Id header")));
    }

    private Mono<DoubleInsuranceCheckResponse> performDoubleInsuranceCheck(String tenantId, String registrationNumber, String chassisNumber) {
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
            return adapter.checkDoubleInsurance(DoubleInsuranceCheckRequest.builder()
                    .registrationNumber(registrationNumber)
                    .chassisNumber(chassisNumber)
                    .build())
                    .doOnNext(response -> log.info("Double insurance check result for {}: hasDuplicate={}", 
                            registrationNumber, response.isHasDuplicate()));
        } else {
            log.error("No integration adapter found for tenant: {}. This is a terminal error.", tenantId);
            return Mono.error(new BusinessException("No integration adapter configured for tenant: " + tenantId));
        }
    }

    public Mono<QuoteResponse> calculateQuote(QuoteRequest request) {
        return getTenantIdOrThrow()
            .flatMap(tenantId -> {
                String quoteId = request.getQuoteId();
                log.info("Calculating quote for tenant: {}, quoteId: {}", tenantId, quoteId);

                return redisTemplate.opsForValue().get("quote_init:" + quoteId)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .flatMap(cachedInitOpt -> {
                            Object cachedInit = cachedInitOpt.orElse(null);
                            if (quoteId != null && cachedInit == null) {
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

                            return pricingEngine.price(ratingContext)
                                    .flatMap(pricingResult -> rateBookSnapshotLoader.loadActive(tenantId)
                                            .map(snapshot -> {
                                                Long rateBookId = (snapshot != null) ? snapshot.rateBookId() : null;
                                                String rateBookVersion = (snapshot != null) ? snapshot.version() : null;
                                                String cacheKey = (snapshot != null) ? snapshot.cacheKey() : null;

                                                return QuoteResponse.builder()
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
                                            })
                                            .flatMap(response -> {
                                                Mono<Void> kycTask = Mono.empty();
                                                if (kyc != null) {
                                                    kycTask = securityContextService.getCurrentUserId()
                                                            .flatMap(userId -> {
                                                                log.info("Upserting customer details for user: {}", userId);
                                                                Mono<Void> upsertCustomer = customerService.createOrUpdateCustomer(userId, CustomerRequest.builder()
                                                                        .fullName(kyc.getFullName())
                                                                        .email(kyc.getEmail())
                                                                        .phoneNumber(kyc.getPhoneNumber())
                                                                        .physicalAddress(kyc.getPhysicalAddress())
                                                                        .build()).then();

                                                                log.info("Associating vehicle {} with user: {}", vehicle.getLicensePlateNumber(), userId);
                                                                Mono<Void> upsertVehicle = userVehicleService.saveOrUpdateVehicle(userId, UserVehicleDto.builder()
                                                                        .registrationNumber(vehicle.getLicensePlateNumber())
                                                                        .vehicleMake(vehicle.getMakeCode())
                                                                        .vehicleModel(vehicle.getModelCode())
                                                                        .yearOfManufacture(vehicle.getYearOfManufacture())
                                                                        .vehicleValue(vehicle.getValuationAmount())
                                                                        .chassisNumber(vehicle.getChassisNumber())
                                                                        .engineNumber(vehicle.getEngineNumber())
                                                                        .build()).then();

                                                                return Mono.when(upsertCustomer, upsertVehicle);
                                                            });
                                                }

                                                return kycTask.then(redisTemplate.opsForValue()
                                                                .set("quote_v2:" + response.getQuoteId(), response, Duration.ofDays(30))
                                                                .doOnSuccess(v -> log.info("Cached quote {} for application conversion", response.getQuoteId()))
                                                                .thenReturn(response));
                                            }));
                        });
            });
    }

    public Mono<QuoteResponse> getQuote(String quoteId) {
        return redisTemplate.opsForValue().get("quote_v2:" + quoteId)
                .map(obj -> (QuoteResponse) obj);
    }

    public Mono<InitiateQuoteResponse> getInitiatedQuote(String quoteId) {
        return redisTemplate.opsForValue().get("quote_init:" + quoteId)
                .map(obj -> (InitiateQuoteResponse) obj);
    }
}
