package com.isec.platform.modules.applications.service.motor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.common.exception.ResourceNotFoundException;
import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.applications.domain.motor.MotorQuoteApplication;
import com.isec.platform.modules.applications.domain.motor.MotorQuoteStatus;
import com.isec.platform.modules.applications.dto.motor.CalculateMotorPremiumRequest;
import com.isec.platform.modules.applications.dto.motor.MotorQuoteResponse;
import com.isec.platform.modules.applications.dto.motor.MpesaInitiationRequest;
import com.isec.platform.modules.applications.mapper.motor.MotorQuoteMapper;
import com.isec.platform.modules.applications.repository.motor.MotorQuoteRepository;
import com.isec.platform.modules.integrations.mpesa.model.MpesaCheckStatusRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatus;
import com.isec.platform.modules.integrations.mpesa.provider.MpesaProviderType;
import com.isec.platform.modules.integrations.quote.model.DraftQuoteResponse;
import com.isec.platform.modules.integrations.quote.model.GetDraftQuoteRequest;
import com.isec.platform.modules.integrations.quote.provider.PartnerQuoteProvider;
import com.isec.platform.modules.integrations.quote.provider.PartnerQuoteProviderFactory;
import com.isec.platform.modules.integrations.quote.provider.QuoteLifecycleCapability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class MotorQuoteOrchestrator {

    private final MotorQuoteRepository repository;
    private final MotorQuoteMapper mapper;
    private final PartnerQuoteProviderFactory partnerFactory;
    private final ObjectMapper objectMapper;

    @Value("${quote.min-payment-percentage:0.35}")
    private double minPaymentPercentage;

    public Mono<MotorQuoteResponse> calculatePremium(CalculateMotorPremiumRequest request) {
        log.info("Starting premium calculation for quoteId: {}, partner: {}", request.getQuoteId(), request.getPartner());
        try {
            log.info("Canonical Premium Calculation Request: {}", objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            log.warn("Failed to log canonical request payload: {}", e.getMessage());
        }

        return TenantContext.getTenantId()
                .switchIfEmpty(Mono.error(new BusinessException("Missing required X-Tenant-Id header")))
                .flatMap(tenantId -> repository.findByQuoteId(request.getQuoteId())
                        .flatMap(existing -> {
                            mapper.updateEntity(existing, request);
                            existing.setTenantId(tenantId);
                            return Mono.just(existing);
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            MotorQuoteApplication newApp = mapper.toEntity(request);
                            newApp.setTenantId(tenantId);
                            return Mono.just(newApp);
                        })))
                .flatMap(app -> {
                    app.setStatus(MotorQuoteStatus.PREMIUM_CALCULATION_IN_PROGRESS);
                    return repository.save(app);
                })
                .flatMap(app -> {
                    PartnerQuoteProvider provider = partnerFactory.getProvider(app.getPartner());
                    return provider.calculatePremium(mapper.toPremiumRequest(app))
                            .flatMap(res -> {
                                app.setStatus(MotorQuoteStatus.PREMIUM_CALCULATED);
                                app.setPremiumResult(serialize(res));
                                app.setRawPartnerResponses(serialize(res)); // Update raw response
                                return repository.save(app);
                            })
                            .onErrorResume(e -> {
                                log.error("Premium calculation failed for quoteId: {}", app.getQuoteId(), e);
                                app.setStatus(MotorQuoteStatus.PREMIUM_CALCULATION_FAILED);
                                return repository.save(app).then(Mono.error(e));
                            });
                })
                .map(mapper::toResponse);
    }

    public Mono<MotorQuoteResponse> acceptQuote(String quoteId) {
        log.info("Accepting quote: {}", quoteId);
        return TenantContext.getTenantId()
                .switchIfEmpty(Mono.error(new BusinessException("Missing required X-Tenant-Id header")))
                .flatMap(tenantId -> repository.findByQuoteId(quoteId)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("MotorQuoteApplication", quoteId)))
                        .flatMap(app -> {
                            app.setTenantId(tenantId);
                            if (app.getStatus() != MotorQuoteStatus.PREMIUM_CALCULATED && app.getStatus() != MotorQuoteStatus.QUOTE_ACCEPTED && app.getStatus() != MotorQuoteStatus.DRAFT_QUOTE_CREATED) {
                                return Mono.error(new BusinessException("Invalid status for quote acceptance: " + app.getStatus()));
                            }
                            app.setStatus(MotorQuoteStatus.QUOTE_ACCEPTED);
                            return repository.save(app);
                        }))
                .flatMap(app -> {
                    PartnerQuoteProvider provider = partnerFactory.getProvider(app.getPartner());
                    if (provider.supportedCapabilities().contains(QuoteLifecycleCapability.CREATE_DRAFT_QUOTE)) {
                        return provider.createDraftQuote(mapper.toDraftQuoteRequest(app))
                                .flatMap(res -> {
                                    app.setStatus(MotorQuoteStatus.DRAFT_QUOTE_CREATED);
                                    app.setDraftQuoteResult(serialize(res));
                                    app.setRawPartnerResponses(serialize(res)); // Update raw response
                                    return repository.save(app);
                                });
                    }
                    return Mono.just(app);
                })
                .map(mapper::toResponse);
    }

    public Mono<MotorQuoteResponse> initiatePayment(String quoteId, MpesaInitiationRequest request) {
        log.info("Initiating payment for quote: {}", quoteId);
        return TenantContext.getTenantId()
                .switchIfEmpty(Mono.error(new BusinessException("Missing required X-Tenant-Id header")))
                .flatMap(tenantId -> repository.findByQuoteId(quoteId)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("MotorQuoteApplication", quoteId)))
                        .flatMap(app -> {
                            app.setTenantId(tenantId);
                            if (request.getKycDetails() != null) {
                                mapper.updateKycDetails(app, request.getKycDetails());
                            }
                            DraftQuoteResponse draft = deserialize(app.getDraftQuoteResult(), DraftQuoteResponse.class);
                            if (draft == null || draft.getDraftQuoteRef() == null) {
                                return Mono.error(new BusinessException("Draft quote reference missing. Acceptance required."));
                            }

                            PartnerQuoteProvider provider = partnerFactory.getProvider(app.getPartner());

                            double requestAmount = getRequestAmount(request, draft);

                            MpesaInitiatePaymentRequest initRequest = MpesaInitiatePaymentRequest.builder()
                                    .partner(MpesaProviderType.valueOf(app.getPartner().name()))
                                    .quoteRef(draft.getDraftQuoteRef())
                                    .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : draft.getClientPhone())
                                    .amount(requestAmount)
                                    .build();

                            return provider.initiatePayment(initRequest)
                                    .flatMap(res -> {
                                        app.setStatus(MotorQuoteStatus.PAYMENT_INITIATED);
                                        app.setPaymentResult(serialize(res));
                                        app.setRawPartnerResponses(serialize(res)); // Update raw response
                                        return repository.save(app);
                                    });
                        }))
                .map(mapper::toResponse);
    }

    private double getRequestAmount(MpesaInitiationRequest request, DraftQuoteResponse draft) {
        double fullAmount = draft.getDraftQuoteAmount().doubleValue();
        double requestAmount = request.getAmount() != null ? request.getAmount() : fullAmount;

        if (requestAmount < fullAmount) {
            double minAmount = fullAmount * minPaymentPercentage;
            if (requestAmount < minAmount) {
                throw new BusinessException(String.format("Minimum payment amount is %.0f%% (KES %.2f) of the total premium", minPaymentPercentage * 100, minAmount));
            }
        }
        return requestAmount;
    }

    public Mono<MotorQuoteResponse> checkPaymentStatus(String quoteId) {
        log.info("Checking payment status for quote: {}", quoteId);
        return TenantContext.getTenantId()
                .switchIfEmpty(Mono.error(new BusinessException("Missing required X-Tenant-Id header")))
                .flatMap(tenantId -> repository.findByQuoteId(quoteId)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("MotorQuoteApplication", quoteId)))
                        .flatMap(app -> {
                            app.setTenantId(tenantId);
                            MpesaCheckStatusRequest statusRequest = buildCheckStatusRequest(app);
                            PartnerQuoteProvider provider = partnerFactory.getProvider(app.getPartner());

                            return provider.checkPaymentStatus(statusRequest)
                                    .flatMap(res -> {
                                        if (res.getStatus() == MpesaPaymentStatus.SUCCESS) {
                                            app.setStatus(MotorQuoteStatus.PAYMENT_SUCCESSFUL);
                                        } else if (res.getStatus() == MpesaPaymentStatus.FAILED) {
                                            app.setStatus(MotorQuoteStatus.PAYMENT_FAILED);
                                        } else {
                                            app.setStatus(MotorQuoteStatus.PAYMENT_PENDING);
                                        }
                                        app.setPaymentResult(serialize(res));
                                        app.setRawPartnerResponses(serialize(res)); // Update raw response
                                        return repository.save(app);
                                    });
                        }))
                .map(mapper::toResponse);
    }

    public Mono<MotorQuoteResponse> getQuoteApplication(String quoteId) {
        return repository.findByQuoteId(quoteId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("MotorQuoteApplication", quoteId)))
                .map(mapper::toResponse);
    }

    private MpesaCheckStatusRequest buildCheckStatusRequest(MotorQuoteApplication app) {
        DraftQuoteResponse draft = deserialize(app.getDraftQuoteResult(), DraftQuoteResponse.class);
        try {
            // Need checkoutId from previous payment result
            com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse initRes =
                    objectMapper.readValue(app.getPaymentResult(), com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse.class);

            return MpesaCheckStatusRequest.builder()
                    .partner(MpesaProviderType.valueOf(app.getPartner().name()))
                    .quoteRef(draft.getDraftQuoteRef())
                    .checkoutId(initRes.getCheckoutId())
                    .build();
        } catch (Exception e) {
            throw new BusinessException("Failed to resolve checkout ID for status check");
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
