package com.isec.platform.modules.applications.service.motor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.common.exception.ResourceNotFoundException;
import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.applications.domain.motor.MotorQuoteApplication;
import com.isec.platform.modules.applications.domain.motor.MotorQuoteStatus;
import com.isec.platform.modules.applications.domain.motor.PaymentMethod;
import com.isec.platform.modules.applications.domain.motor.PaymentVerificationMode;
import com.isec.platform.modules.applications.dto.InitiateQuoteResponse;
import com.isec.platform.modules.applications.dto.QuoteRequest;
import com.isec.platform.modules.applications.dto.motor.CalculateMotorPremiumRequest;
import com.isec.platform.modules.applications.dto.motor.MotorPaymentResult;
import com.isec.platform.modules.applications.dto.motor.MotorQuoteResponse;
import com.isec.platform.modules.applications.dto.motor.MpesaInitiationRequest;
import com.isec.platform.modules.applications.mapper.motor.MotorQuoteMapper;
import com.isec.platform.modules.applications.repository.motor.MotorQuoteRepository;
import com.isec.platform.modules.integrations.mpesa.model.*;
import com.isec.platform.modules.integrations.mpesa.provider.MpesaPaymentProvider;
import com.isec.platform.modules.integrations.mpesa.provider.MpesaProviderFactory;
import com.isec.platform.modules.integrations.mpesa.provider.MpesaProviderType;
import com.isec.platform.modules.integrations.quote.model.DraftQuoteResponse;
import com.isec.platform.modules.integrations.quote.model.DraftQuoteStatus;
import com.isec.platform.modules.integrations.quote.provider.PartnerQuoteProvider;
import com.isec.platform.modules.integrations.quote.provider.PartnerQuoteProviderFactory;
import com.isec.platform.modules.integrations.quote.provider.QuoteLifecycleCapability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class MotorQuoteOrchestrator {

    private final MotorQuoteRepository repository;
    private final MotorQuoteMapper mapper;
    private final PartnerQuoteProviderFactory partnerFactory;
    private final MpesaProviderFactory mpesaProviderFactory;
    private final PartnerPaymentAccountService paymentAccountService;
    private final ManualPaymentInstructionService manualPaymentInstructionService;
    private final ObjectMapper objectMapper;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

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
                            if (existing.getStatus() == MotorQuoteStatus.POLICY_ISSUED ||
                                    existing.getStatus() == MotorQuoteStatus.POLICY_ISSUANCE_IN_PROGRESS) {
                                return Mono.error(new BusinessException("Cannot recalculate premium for an already issued policy or one in progress of issuance."));
                            }
                            mapper.updateEntity(existing, request);
                            existing.setTenantId(tenantId);
                            return Mono.just(existing);
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            MotorQuoteApplication newApp = mapper.toEntity(request);
                            newApp.setTenantId(tenantId);
                            return Mono.just(newApp);
                        })))
                .flatMap(app -> redisTemplate.opsForValue().get("quote_init:" + app.getQuoteId())
                        .cast(InitiateQuoteResponse.class)
                        .doOnNext(initResponse -> {
                            if (initResponse.getDoubleInsuranceCheck() != null) {
                                try {
                                    app.setDmvicCheckResult(objectMapper.writeValueAsString(initResponse.getDoubleInsuranceCheck()));
                                } catch (JsonProcessingException e) {
                                    log.warn("Failed to serialize DMVIC check result for quoteId: {}", app.getQuoteId());
                                }
                            }
                        })
                        .thenReturn(app))
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
                            if (app.getStatus() == MotorQuoteStatus.POLICY_ISSUED ||
                                    app.getStatus() == MotorQuoteStatus.POLICY_ISSUANCE_IN_PROGRESS) {
                                return Mono.error(new BusinessException("Cannot accept quote for an already issued policy or one in progress of issuance."));
                            }
                            if (app.getStatus() != MotorQuoteStatus.PREMIUM_CALCULATED && app.getStatus() != MotorQuoteStatus.QUOTE_ACCEPTED && app.getStatus() != MotorQuoteStatus.DRAFT_QUOTE_CREATED) {
                                return Mono.error(new BusinessException("Invalid status for quote acceptance: " + app.getStatus()));
                            }

                            app.setStatus(MotorQuoteStatus.QUOTE_ACCEPTED);
                            return repository.save(app);
                        }))
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
                            if (app.getStatus() == MotorQuoteStatus.POLICY_ISSUED ||
                                    app.getStatus() == MotorQuoteStatus.POLICY_ISSUANCE_IN_PROGRESS) {
                                return Mono.error(new BusinessException("Cannot initiate payment for an already issued policy or one in progress of issuance."));
                            }

                            // 1. Validate KYC/client details presence
                            if (request.getKycDetails() == null) {
                                // Try to see if we already have them
                                QuoteRequest.KycDetails existingKyc = deserialize(app.getKycDetails(), QuoteRequest.KycDetails.class);
                                if (existingKyc == null || StringUtils.isBlank(existingKyc.getFullName())) {
                                    return Mono.error(new BusinessException("KYC details are required before initiating payment."));
                                }
                            } else {
                                // Validate required fields in request KYC
                                validateKyc(request.getKycDetails());
                                mapper.updateKycDetails(app, request.getKycDetails());
                            }

                            return createDraftQuoteIfMissing(app)
                                    .flatMap(appWithDraft -> {
                                        DraftQuoteResponse draft = deserialize(appWithDraft.getDraftQuoteResult(), DraftQuoteResponse.class);
                                        if (draft == null || draft.getDraftQuoteRef() == null) {
                                            return Mono.error(new BusinessException("Draft quote reference missing. Failed to create draft quote with partner."));
                                        }

                                        PartnerQuoteProvider provider = partnerFactory.getProvider(appWithDraft.getPartner());
                                        double requestAmount = getRequestAmount(request, draft);

                                        MpesaInitiatePaymentRequest initRequest = MpesaInitiatePaymentRequest.builder()
                                                .partner(MpesaProviderType.valueOf(appWithDraft.getPartner().name()))
                                                .quoteRef(draft.getDraftQuoteRef())
                                                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : draft.getClientPhone())
                                                .amount(requestAmount)
                                                .build();

                                        // Get manual instructions
                                        return paymentAccountService.getDefaultActiveAccount(appWithDraft.getPartner(), "MPESA", PaymentMethod.MPESA_PAYBILL)
                                                .flatMap(account -> {
                                                    QuoteRequest.VehicleDetails vehicle = deserialize(appWithDraft.getVehicleDetails(), QuoteRequest.VehicleDetails.class);
                                                    String accountNumber = vehicle != null && vehicle.getLicensePlateNumber() != null ? 
                                                            vehicle.getLicensePlateNumber() : draft.getDraftQuoteRef();
                                                    
                                                    var manualInstructions = manualPaymentInstructionService.getInstructions(
                                                            PaymentMethod.MPESA_PAYBILL, 
                                                            account.getBusinessNumber(), 
                                                            accountNumber, 
                                                            BigDecimal.valueOf(requestAmount), 
                                                            "KES"
                                                    );

                                                    return provider.initiatePayment(initRequest)
                                                            .flatMap(res -> {
                                                                MotorPaymentResult result = MotorPaymentResult.builder()
                                                                        .paymentMethod(PaymentMethod.MPESA_STK)
                                                                        .verificationMode(PaymentVerificationMode.STK_STATUS)
                                                                        .status(getMotorQuoteStatusFromPaymentStatus(res.getStatus()))
                                                                        .checkoutId(res.getCheckoutId())
                                                                        .amount(BigDecimal.valueOf(requestAmount))
                                                                        .businessNumber(account.getBusinessNumber())
                                                                        .accountNumber(accountNumber)
                                                                        .instructions(manualInstructions.getInstructions())
                                                                        .rawResponse(serialize(res))
                                                                        .build();

                                                                appWithDraft.setStatus(MotorQuoteStatus.PAYMENT_INITIATED);
                                                                appWithDraft.setPaymentResult(serialize(result));
                                                                appWithDraft.setRawPartnerResponses(serialize(res));
                                                                return repository.save(appWithDraft);
                                                            });
                                                });
                                    });
                        }))
                .map(mapper::toResponse);
    }

    private void validateKyc(QuoteRequest.KycDetails kyc) {
        if (StringUtils.isBlank(kyc.getFullName())) {
            throw new BusinessException("Client name must not be blank");
        }
        if (StringUtils.isBlank(kyc.getPhoneNumber())) {
            throw new BusinessException("Client phone number must not be blank");
        }
        if (StringUtils.isBlank(kyc.getEmail())) {
            throw new BusinessException("Client email must not be blank");
        }
        if (StringUtils.isBlank(kyc.getIdNumber())) {
            throw new BusinessException("Client ID number must not be blank");
        }
    }

    private Mono<MotorQuoteApplication> createDraftQuoteIfMissing(MotorQuoteApplication app) {
        if (app.getStatus() == MotorQuoteStatus.DRAFT_QUOTE_CREATED ||
                app.getStatus() == MotorQuoteStatus.PAYMENT_INITIATED ||
                app.getStatus() == MotorQuoteStatus.PAYMENT_PENDING ||
                app.getStatus() == MotorQuoteStatus.PAYMENT_SUCCESSFUL) {

            DraftQuoteResponse existing = deserialize(app.getDraftQuoteResult(), DraftQuoteResponse.class);
            if (existing != null && existing.getDraftQuoteRef() != null) {
                log.info("Draft quote already exists for quote: {}. Reusing reference: {}", app.getQuoteId(), existing.getDraftQuoteRef());
                return Mono.just(app);
            }
        }

        PartnerQuoteProvider provider = partnerFactory.getProvider(app.getPartner());
        if (provider.supportedCapabilities().contains(QuoteLifecycleCapability.CREATE_DRAFT_QUOTE)) {
            log.info("Creating Sanlam draft quote for quote: {}", app.getQuoteId());
            return mapper.toDraftQuoteRequest(app)
                    .flatMap(provider::createDraftQuote)
                    .flatMap(res -> {
                        app.setStatus(MotorQuoteStatus.DRAFT_QUOTE_CREATED);
                        app.setDraftQuoteResult(serialize(res));
                        app.setRawPartnerResponses(serialize(res));
                        return repository.save(app);
                    });
        }
        return Mono.just(app);
    }

    private double getRequestAmount(MpesaInitiationRequest request, DraftQuoteResponse draft) {
        double fullAmount = draft.getDraftQuoteAmount().doubleValue();
        double requestAmount = request.getAmount() != null ? request.getAmount() : fullAmount;

        if (requestAmount < fullAmount) {
            double minAmount = fullAmount * minPaymentPercentage;
            if (requestAmount < minAmount) {
                throw new BusinessException(String.format("Minimum payment amount is %.0f%% (KES %.2f) of the total premium (KES %.2f)", minPaymentPercentage * 100, minAmount, fullAmount));
            }
        }
        return requestAmount;
    }

    public Mono<MotorQuoteResponse> checkPaymentStatus(String quoteId, PaymentMethod method, String receipt) {
        log.info("Checking payment status for quote: {}, method: {}, receipt: {}", quoteId, method, receipt);
        return TenantContext.getTenantId()
                .switchIfEmpty(Mono.error(new BusinessException("Missing required X-Tenant-Id header")))
                .flatMap(tenantId -> repository.findByQuoteId(quoteId)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("MotorQuoteApplication", quoteId)))
                        .flatMap(app -> {
                            app.setTenantId(tenantId);

                            if (app.getStatus() == MotorQuoteStatus.POLICY_ISSUANCE_IN_PROGRESS) {
                                log.info("Policy issuance in progress for quote: {}. Skipping payment status check.", quoteId);
                                return Mono.just(app);
                            }

                            if (method == PaymentMethod.MPESA_PAYBILL) {
                                if (StringUtils.isBlank(receipt)) {
                                    return Mono.error(new BusinessException("Receipt is required for MPESA_PAYBILL verification"));
                                }
                                return verifyPaybillReceipt(app, receipt);
                            } else {
                                return checkStkPushStatus(app);
                            }
                        })
                        .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                                .filter(throwable -> throwable instanceof OptimisticLockingFailureException)
                                .doBeforeRetry(retrySignal -> log.warn("Optimistic locking failure for quote: {}. Retrying... (Attempt {})", quoteId, retrySignal.totalRetries() + 1))))
                .map(mapper::toResponse);
    }

    private Mono<MotorQuoteApplication> checkStkPushStatus(MotorQuoteApplication app) {
        MpesaCheckStatusRequest statusRequest = buildCheckStatusRequest(app);
        PartnerQuoteProvider provider = partnerFactory.getProvider(app.getPartner());

        return provider.checkPaymentStatus(statusRequest)
                .flatMap(res -> {
                    MotorQuoteStatus newStatus = getMotorQuoteStatus(res);
                    
                    // Preserve existing manual info if present
                    MotorPaymentResult existingResult = deserialize(app.getPaymentResult(), MotorPaymentResult.class);
                    MotorPaymentResult result = MotorPaymentResult.builder()
                            .paymentMethod(PaymentMethod.MPESA_STK)
                            .verificationMode(PaymentVerificationMode.STK_STATUS)
                            .status(newStatus)
                            .checkoutId(res.getCheckoutId())
                            .receipt(res.getReceiptNumber())
                            .rawResponse(serialize(res))
                            .amount(existingResult != null ? existingResult.getAmount() : null)
                            .businessNumber(existingResult != null ? existingResult.getBusinessNumber() : null)
                            .accountNumber(existingResult != null ? existingResult.getAccountNumber() : null)
                            .instructions(existingResult != null ? existingResult.getInstructions() : null)
                            .build();

                    String serializedResult = serialize(result);
                    if ((app.getStatus() == newStatus && StringUtils.equals(app.getPaymentResult(), serializedResult)) ||
                            (app.getStatus() == MotorQuoteStatus.POLICY_ISSUED && newStatus == MotorQuoteStatus.PAYMENT_SUCCESSFUL)) {
                        return Mono.just(app);
                    }

                    app.setStatus(newStatus);
                    app.setPaymentResult(serializedResult);
                    app.setRawPartnerResponses(serialize(res));
                    return repository.save(app);
                })
                .flatMap(this::triggerAutomaticPolicyIssuance);
    }

    private Mono<MotorQuoteApplication> verifyPaybillReceipt(MotorQuoteApplication app, String receipt) {
        DraftQuoteResponse draft = deserialize(app.getDraftQuoteResult(), DraftQuoteResponse.class);
        if (draft == null || draft.getDraftQuoteRef() == null) {
            return Mono.error(new BusinessException("Draft quote reference missing for receipt verification"));
        }

        MpesaVerifyReceiptRequest verifyRequest = MpesaVerifyReceiptRequest.builder()
                .quoteRef(draft.getDraftQuoteRef())
                .receipt(receipt)
                .numberOfInstallments(1) // Default to 1
                .build();

        MpesaPaymentProvider mpesaProvider = mpesaProviderFactory.getProvider(MpesaProviderType.valueOf(app.getPartner().name()));

        return mpesaProvider.verifyReceiptAndMap(verifyRequest)
                .flatMap(res -> {
                    MotorQuoteStatus newStatus = getMotorQuoteStatus(res);
                    
                    MotorPaymentResult existingResult = deserialize(app.getPaymentResult(), MotorPaymentResult.class);
                    
                    MotorPaymentResult result = MotorPaymentResult.builder()
                            .paymentMethod(PaymentMethod.MPESA_PAYBILL)
                            .verificationMode(PaymentVerificationMode.RECEIPT_VERIFICATION)
                            .status(newStatus)
                            .receipt(receipt)
                            .amount(res.getAmount() != null ? BigDecimal.valueOf(res.getAmount()) : (existingResult != null ? existingResult.getAmount() : null))
                            .paidAt(res.getPaidAt())
                            .checkoutId(res.getCheckoutId())
                            .rawResponse(serialize(res))
                            .businessNumber(existingResult != null ? existingResult.getBusinessNumber() : null)
                            .accountNumber(existingResult != null ? existingResult.getAccountNumber() : null)
                            .instructions(existingResult != null ? existingResult.getInstructions() : null)
                            .build();

                    app.setStatus(newStatus);
                    app.setPaymentResult(serialize(result)); // Keep MotorPaymentResult as the primary result
                    app.setRawPartnerResponses(serialize(res));
                    
                    // We also need to store the Manual Payment specific info somewhere if we want to keep it, 
                    // but MpesaPaymentStatusResponse should have enough (receipt, amount, paidAt).
                    // If we need the full MotorPaymentResult, we should probably change issuePolicyInternal to handle it.
                    
                    return repository.save(app);
                })
                .flatMap(this::triggerAutomaticPolicyIssuance);
    }

    private Mono<MotorQuoteApplication> triggerAutomaticPolicyIssuance(MotorQuoteApplication app) {
        if (app.getStatus() == MotorQuoteStatus.PAYMENT_SUCCESSFUL) {
            log.info("Payment successful for quote: {}. Triggering automatic policy issuance.", app.getQuoteId());
            return issuePolicyInternal(app)
                    .doOnNext(finalApp -> log.info("Automatic policy issuance completed for quote: {}. Final status: {}", app.getQuoteId(), finalApp.getStatus()));
        }
        return Mono.just(app);
    }

    private MotorQuoteStatus getMotorQuoteStatus(MpesaPaymentStatusResponse res) {
        return getMotorQuoteStatusFromPaymentStatus(res.getStatus());
    }

    private MpesaPaymentStatus getMpesaPaymentStatus(MotorQuoteStatus status) {
        if (status == null) return null;
        try {
            return MpesaPaymentStatus.valueOf(status.name());
        } catch (Exception e) {
            // Map common MotorQuoteStatus to MpesaPaymentStatus
            return switch (status) {
                case PAYMENT_SUCCESSFUL -> MpesaPaymentStatus.SUCCESS;
                case PAYMENT_FAILED -> MpesaPaymentStatus.FAILED;
                case PAYMENT_INITIATED, PAYMENT_PENDING -> MpesaPaymentStatus.PENDING;
                default -> MpesaPaymentStatus.UNKNOWN;
            };
        }
    }

    private MotorQuoteStatus getMotorQuoteStatusFromPaymentStatus(MpesaPaymentStatus status) {
        if (status == MpesaPaymentStatus.SUCCESS) {
            return MotorQuoteStatus.PAYMENT_SUCCESSFUL;
        } else if (status == MpesaPaymentStatus.FAILED) {
            return MotorQuoteStatus.PAYMENT_FAILED;
        } else {
            return MotorQuoteStatus.PAYMENT_PENDING;
        }
    }

    public Mono<MotorQuoteResponse> getQuoteApplication(String quoteId) {
        return repository.findByQuoteId(quoteId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("MotorQuoteApplication", quoteId)))
                .map(mapper::toResponse);
    }

    public Mono<MotorQuoteResponse> issuePolicy(String quoteId) {
        log.info("Starting policy issuance for quoteId: {}", quoteId);
        return TenantContext.getTenantId()
                .switchIfEmpty(Mono.error(new BusinessException("Missing required X-Tenant-Id header")))
                .flatMap(tenantId -> repository.findByQuoteId(quoteId)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("MotorQuoteApplication", quoteId)))
                        .flatMap(app -> {
                            app.setTenantId(tenantId);
                            return issuePolicyInternal(app);
                        }))
                .map(mapper::toResponse);
    }

    private Mono<MotorQuoteApplication> issuePolicyInternal(MotorQuoteApplication app) {
        log.info("Internal policy issuance for quoteId: {}", app.getQuoteId());
        if (app.getStatus() == MotorQuoteStatus.POLICY_ISSUED) {
            log.info("Policy already issued for quoteId: {}", app.getQuoteId());
            return Mono.just(app);
        }
        if (app.getStatus() != MotorQuoteStatus.PAYMENT_SUCCESSFUL) {
            return Mono.error(new BusinessException("Policy can only be issued after successful payment. Current status: " + app.getStatus()));
        }

        app.setStatus(MotorQuoteStatus.POLICY_ISSUANCE_IN_PROGRESS);
        return repository.save(app)
                .flatMap(savedApp -> {
                    PartnerQuoteProvider provider = partnerFactory.getProvider(savedApp.getPartner());
                    if (!provider.supportedCapabilities().contains(QuoteLifecycleCapability.ISSUE_POLICY)) {
                        return Mono.error(new BusinessException("Partner does not support policy issuance: " + savedApp.getPartner()));
                    }

                    DraftQuoteResponse draftQuote = deserialize(savedApp.getDraftQuoteResult(), DraftQuoteResponse.class);
                    mapper.mergeLatestKyc(draftQuote, savedApp);
                    MpesaPaymentStatusResponse paymentStatus = resolvePaymentStatus(savedApp);

                    return provider.issuePolicy(savedApp.getQuoteId(), draftQuote, paymentStatus)
                            .flatMap(result -> {
                                savedApp.setStatus(MotorQuoteStatus.POLICY_ISSUED);
                                savedApp.setPolicyIssuanceResult(serialize(result));

                                // Update partner references with quotSysId if available
                                if (result.getMetadata() != null && result.getMetadata().containsKey("quot_sys_id")) {
                                    savedApp.setPartnerReferences(serialize(result.getMetadata()));
                                }

                                // NEW: Update draftQuoteResult with latest state from result metadata if available
                                if (result.getMetadata() != null && result.getMetadata().containsKey("draft_quote_sys_id")) {
                                    DraftQuoteResponse.DraftQuoteResponseBuilder draftBuilder = DraftQuoteResponse.builder()
                                            .draftQuoteSysId(((Number) result.getMetadata().get("draft_quote_sys_id")).longValue())
                                            .draftQuoteRef((String) result.getMetadata().get("draft_quote_ref"));

                                    if (result.getMetadata().get("quot_sys_id") != null) {
                                        draftBuilder.quotSysId(((Number) result.getMetadata().get("quot_sys_id")).longValue());
                                    }

                                    if (result.getMetadata().get("status") != null) {
                                        String statusStr = (String) result.getMetadata().get("status");
                                        try {
                                            draftBuilder.status(DraftQuoteStatus.valueOf(statusStr.toUpperCase()));
                                        } catch (Exception e) {
                                            log.warn("Unknown draft quote status received from metadata: {}. Defaulting to UNKNOWN", statusStr);
                                            draftBuilder.status(DraftQuoteStatus.UNKNOWN);
                                        }
                                    }

                                    savedApp.setDraftQuoteResult(serialize(draftBuilder.build()));
                                }

                                savedApp.setRawPartnerResponses(serialize(result));
                                return repository.save(savedApp);
                            })
                            .onErrorResume(e -> {
                                log.error("Policy issuance failed for quoteId: {}", savedApp.getQuoteId(), e);
                                savedApp.setStatus(MotorQuoteStatus.POLICY_ISSUANCE_FAILED);
                                return repository.save(savedApp).then(Mono.error(e));
                            });
                });
    }

    private MpesaPaymentStatusResponse resolvePaymentStatus(MotorQuoteApplication app) {
        String json = app.getPaymentResult();
        if (json == null) return null;

        // Try as MpesaPaymentStatusResponse first
        MpesaPaymentStatusResponse statusRes = deserialize(json, MpesaPaymentStatusResponse.class);
        if (statusRes != null && statusRes.getStatus() != null) {
            return statusRes;
        }

        // Try as MotorPaymentResult and map it
        MotorPaymentResult motorRes = deserialize(json, MotorPaymentResult.class);
        if (motorRes != null) {
            return MpesaPaymentStatusResponse.builder()
                    .provider(MpesaProviderType.valueOf(app.getPartner().name()))
                    .status(getMpesaPaymentStatus(motorRes.getStatus()))
                    .message("Resolved from MotorPaymentResult")
                    .receiptNumber(motorRes.getReceipt())
                    .amount(motorRes.getAmount() != null ? motorRes.getAmount().doubleValue() : 0.0)
                    .paidAt(motorRes.getPaidAt())
                    .checkoutId(motorRes.getCheckoutId() != null ? motorRes.getCheckoutId() : "MANUAL_" + app.getQuoteId())
                    .rawResponse(motorRes.getRawResponse())
                    .build();
        }

        // Try as Initiation response as fallback
        try {
            com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse initRes =
                    objectMapper.readValue(json, com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse.class);
            if (initRes != null && initRes.getStatus() != null) {
                return MpesaPaymentStatusResponse.builder()
                        .provider(initRes.getProvider())
                        .status(initRes.getStatus())
                        .message(initRes.getMessage())
                        .checkoutId(initRes.getCheckoutId())
                        .rawResponse(initRes.getRawResponse())
                        .build();
            }
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }

    private MpesaCheckStatusRequest buildCheckStatusRequest(MotorQuoteApplication app) {
        DraftQuoteResponse draft = deserialize(app.getDraftQuoteResult(), DraftQuoteResponse.class);
        String checkoutId = null;

        // Try to resolve from MotorPaymentResult first (New structure)
        MotorPaymentResult motorRes = deserialize(app.getPaymentResult(), MotorPaymentResult.class);
        if (motorRes != null && motorRes.getCheckoutId() != null) {
            checkoutId = motorRes.getCheckoutId();
        }

        if (checkoutId == null) {
            try {
                // Try as Status response (legacy/fallback)
                MpesaPaymentStatusResponse statusRes = objectMapper.readValue(app.getPaymentResult(), MpesaPaymentStatusResponse.class);
                checkoutId = statusRes.getCheckoutId();
            } catch (Exception e) {
                // Ignore and try Initiation response
            }
        }

        if (checkoutId == null) {
            try {
                // Try as Initiation response (legacy/fallback)
                com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse initRes =
                        objectMapper.readValue(app.getPaymentResult(), com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse.class);
                checkoutId = initRes.getCheckoutId();
            } catch (Exception e) {
                log.error("Failed to deserialize payment initiation result for quoteId: {}", app.getQuoteId());
            }
        }

        if (checkoutId == null) {
            throw new BusinessException("Failed to resolve checkout ID for status check");
        }

        return MpesaCheckStatusRequest.builder()
                .partner(MpesaProviderType.valueOf(app.getPartner().name()))
                .quoteRef(draft.getDraftQuoteRef())
                .checkoutId(checkoutId)
                .build();
    }

    public String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public <T> T deserialize(String json, Class<T> clazz) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
