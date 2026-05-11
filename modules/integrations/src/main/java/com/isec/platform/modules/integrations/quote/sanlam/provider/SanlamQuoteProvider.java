package com.isec.platform.modules.integrations.quote.sanlam.provider;

import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.modules.integrations.mpesa.model.MpesaCheckStatusRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatusResponse;
import com.isec.platform.modules.integrations.mpesa.sanlam.service.SanlamMpesaProvider;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationRequest;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationResponse;
import com.isec.platform.modules.integrations.premium.sanlam.provider.SanlamPremiumCalculationProvider;
import com.isec.platform.modules.integrations.quote.model.*;
import com.isec.platform.modules.integrations.quote.provider.PartnerQuoteProvider;
import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import com.isec.platform.modules.integrations.quote.provider.QuoteLifecycleCapability;
import com.isec.platform.modules.integrations.quote.sanlam.client.SanlamDraftQuoteClient;
import com.isec.platform.modules.integrations.quote.sanlam.client.SanlamPolicyClient;
import com.isec.platform.modules.integrations.quote.sanlam.dto.SanlamEmailRequest;
import com.isec.platform.modules.integrations.quote.sanlam.mapper.SanlamDraftQuoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class SanlamQuoteProvider implements PartnerQuoteProvider {

    private final SanlamDraftQuoteClient client;
    private final SanlamPolicyClient policyClient;
    private final SanlamDraftQuoteMapper mapper;
    private final SanlamPremiumCalculationProvider premiumProvider;
    private final SanlamMpesaProvider mpesaProvider;

    @Override
    public PartnerType providerType() {
        return PartnerType.SANLAM;
    }

    @Override
    public Set<QuoteLifecycleCapability> supportedCapabilities() {
        return Set.of(
                QuoteLifecycleCapability.CALCULATE_PREMIUM,
                QuoteLifecycleCapability.CREATE_DRAFT_QUOTE,
                QuoteLifecycleCapability.GET_DRAFT_QUOTE,
                QuoteLifecycleCapability.INITIATE_PAYMENT,
                QuoteLifecycleCapability.CHECK_PAYMENT_STATUS,
                QuoteLifecycleCapability.ISSUE_POLICY
        );
    }

    @Override
    public Mono<PremiumCalculationResponse> calculatePremium(PremiumCalculationRequest request) {
        return premiumProvider.calculatePremium(request);
    }

    @Override
    public Mono<DraftQuoteResponse> createDraftQuote(DraftQuoteRequest request) {
        validateDraftQuoteRequest(request);
        return Mono.defer(() -> {
            if (request.getDraftQuoteSysId() != null && request.getDraftQuoteRef() != null) {
                log.info("Sanlam draft quote already exists. Refreshing existing draft quote. draftQuoteSysId={}, draftQuoteRef={}",
                        request.getDraftQuoteSysId(), request.getDraftQuoteRef());

                return getDraftQuote(GetDraftQuoteRequest.builder()
                        .provider(PartnerType.SANLAM)
                        .draftQuoteSysId(request.getDraftQuoteSysId())
                        .build())
                        .doOnNext(response -> log.info("Sanlam draft quote refreshed successfully. draftQuoteSysId={}, draftQuoteRef={}, quotSysId={}",
                                response.getDraftQuoteSysId(), response.getDraftQuoteRef(), response.getQuotSysId()))
                        .onErrorResume(error -> {
                            log.error("Failed to refresh existing Sanlam draft quote. draftQuoteSysId={}. Error: {}",
                                    request.getDraftQuoteSysId(), error.getMessage());
                            return Mono.error(new BusinessException("Failed to refresh existing Sanlam draft quote. Please retry later."));
                        });
            }

            log.info("No existing Sanlam draft quote found. Creating new draft quote for client: {}", request.getClientName());
            long startTime = Instant.now().toEpochMilli();

            return client.createDraftQuote(mapper.toSanlamRequest(request))
                    .flatMap(createResponse -> {
                        log.info("Sanlam draft quote created. SysId: {}, Ref: {}. Fetching canonical state.",
                                createResponse.getDraftQuoteSysId(), createResponse.getDraftQuoteRef());
                        return client.getDraftQuote(createResponse.getDraftQuoteSysId());
                    })
                    .map(mapper::toCommonResponse)
                    .doOnNext(response -> {
                        long latency = Instant.now().toEpochMilli() - startTime;
                        log.info("Sanlam draft quote creation and refresh completed. Ref: {}, Latency: {}ms",
                                response.getDraftQuoteRef(), latency);
                    })
                    .doOnError(error -> log.error("Sanlam draft quote creation failed. Error: {}", error.getMessage()));
        });
    }

    @Override
    public Mono<DraftQuoteResponse> getDraftQuote(GetDraftQuoteRequest request) {
        return Mono.fromRunnable(() -> validateGetDraftQuoteRequest(request))
                .then(Mono.defer(() -> {
                    long startTime = Instant.now().toEpochMilli();
                    log.info("Fetching Sanlam draft quote. SysId: {}", request.getDraftQuoteSysId());

                    return client.getDraftQuote(request.getDraftQuoteSysId())
                            .map(mapper::toCommonResponse)
                            .doOnNext(response -> {
                                long latency = Instant.now().toEpochMilli() - startTime;
                                log.info("Sanlam draft quote fetch completed. Ref: {}, Latency: {}ms",
                                        response.getDraftQuoteRef(), latency);
                            })
                            .doOnError(error -> log.error("Sanlam draft quote fetch failed. Error: {}", error.getMessage()));
                }));
    }

    @Override
    public Mono<MpesaInitiatePaymentResponse> initiatePayment(MpesaInitiatePaymentRequest request) {
        return mpesaProvider.initiatePayment(request);
    }

    @Override
    public Mono<MpesaPaymentStatusResponse> checkPaymentStatus(MpesaCheckStatusRequest request) {
        return mpesaProvider.checkStatus(request);
    }

    @Override
    public Mono<PolicyIssuanceResult> issuePolicy(String quoteId, DraftQuoteResponse draftQuote, MpesaPaymentStatusResponse paymentStatus) {
        return Mono.defer(() -> {
            log.info("Starting Sanlam policy issuance for quoteId: {}, draftQuoteSysId: {}", quoteId, draftQuote.getDraftQuoteSysId());

            validatePolicyIssuance(draftQuote, paymentStatus);

            // Idempotency: Check if this receipt is already in the draft quote
            if (isPaymentAlreadySynced(draftQuote, paymentStatus)) {
                log.info("Payment for receipt {} already synced for draft quote {}. Refreshing state.", 
                        paymentStatus.getReceiptNumber(), draftQuote.getDraftQuoteSysId());
                return getDraftQuote(GetDraftQuoteRequest.builder()
                        .provider(PartnerType.SANLAM)
                        .draftQuoteSysId(draftQuote.getDraftQuoteSysId())
                        .build())
                        .map(latestDraft -> mapper.toPolicyIssuanceResult(mapper.toSanlamDraftQuoteResponse(latestDraft), null));
            }

            return policyClient.updateDraftQuote(draftQuote.getDraftQuoteSysId(), mapper.toUpdateDraftQuoteRequest(draftQuote, paymentStatus))
                    .flatMap(updateResponse -> {
                        log.info("Draft quote updated successfully. draftQuoteSysId={}, quotSysId={}", 
                                updateResponse.getDraftQuoteSysId(), updateResponse.getQuotSysId());

                        // Flow requirement: Then GET /quotes/draft_quote/{draft_quote_sys_id}
                        return client.getDraftQuote(draftQuote.getDraftQuoteSysId())
                                .flatMap(latestDraft -> {
                                    Long quotSysId = latestDraft.getQuotSysId();
                                    
                                    if (quotSysId == null) {
                                        log.warn("Sanlam policy issuance incomplete. quot_sys_id is still null. Keeping status as pending and skipping document dispatch.");
                                        Map<String, Object> metadata = new HashMap<>();
                                        metadata.put("draft_quote_sys_id", latestDraft.getDraftQuoteSysId());
                                        metadata.put("draft_quote_ref", latestDraft.getDraftQuoteRef());
                                        metadata.put("quot_sys_id", quotSysId);
                                        metadata.put("status", latestDraft.getStatus());

                                        return Mono.just(PolicyIssuanceResult.builder()
                                                .status("PAYMENT_SYNCED")
                                                .message("Payment synced, policy conversion pending")
                                                .externalReference(latestDraft.getDraftQuoteRef())
                                                .metadata(metadata)
                                                .build());
                                    }

                                    SanlamEmailRequest emailRequest = SanlamEmailRequest.builder()
                                            .quotSysId(quotSysId)
                                            .includeReceipt(true)
                                            .includeDebitNote(true)
                                            .recipientEmail(draftQuote.getClientEmail())
                                            .build();

                                    log.info("Proceeding to send documents for quotSysId: {}", quotSysId);
                                    return policyClient.sendDocuments(emailRequest)
                                            .map(emailResponse -> {
                                                log.info("Insurance documents email response: {}", emailResponse.getMessage());
                                                return mapper.toPolicyIssuanceResult(latestDraft, emailResponse);
                                            })
                                            .onErrorResume(e -> {
                                                log.error("Failed to send insurance documents for quotSysId: {}. Error: {}", quotSysId, e.getMessage());
                                                return Mono.just(mapper.toPolicyIssuanceResult(latestDraft, null));
                                            });
                                });
                    })
                    .doOnNext(result -> log.info("Sanlam policy issuance result for quoteId {}: {}", quoteId, result.getStatus()))
                    .onErrorResume(e -> {
                        log.error("Sanlam policy issuance failed for quoteId: {}. Error: {}", quoteId, e.getMessage());
                        return Mono.error(e);
                    });
        });
    }

    private boolean isPaymentAlreadySynced(DraftQuoteResponse draftQuote, MpesaPaymentStatusResponse paymentStatus) {
        QuotePaymentSummary summary = draftQuote.getPaymentSummary();
        if (summary == null || summary.getTransactions() == null) {
            return false;
        }
        
        String receipt = paymentStatus.getReceiptNumber();
        String checkoutId = paymentStatus.getCheckoutId();
        
        return summary.getTransactions().stream()
                .anyMatch(tx -> {
                    if (tx instanceof Map) {
                        Map<?, ?> txMap = (Map<?, ?>) tx;
                        return receipt.equals(txMap.get("receipt")) || checkoutId.equals(txMap.get("checkout_id"));
                    }
                    return false;
                });
    }

    private void validatePolicyIssuance(DraftQuoteResponse draftQuote, MpesaPaymentStatusResponse paymentStatus) {
        Assert.notNull(draftQuote, "Draft quote must not be null");
        Assert.notNull(paymentStatus, "Payment status must not be null");
        Assert.notNull(draftQuote.getDraftQuoteSysId(), "draft_quote_sys_id is missing");
        Assert.notNull(draftQuote.getDraftQuoteRef(), "draft_quote_ref is missing");

        // Payment validation
        Assert.notNull(paymentStatus.getCheckoutId(), "checkout_id is missing");
        Assert.notNull(paymentStatus.getReceiptNumber(), "receipt is missing");
        Assert.notNull(paymentStatus.getAmount(), "payment amount is missing");
        Assert.isTrue(paymentStatus.getAmount() > 0, "payment amount must be greater than zero");
        Assert.isTrue(paymentStatus.getStatus() == com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatus.SUCCESS, "Payment must be successful");

        QuotePaymentSummary summary = draftQuote.getPaymentSummary();
        if (summary != null) {
            log.info("Validating payment for quoteId: {}. Receipt: {}, Amount: {}, Installment Count: {}, Remaining Balance: {}",
                    draftQuote.getDraftQuoteRef(), paymentStatus.getReceiptNumber(), paymentStatus.getAmount(),
                    summary.getInstallmentCount(), summary.getRemainingBalance());
        }
    }

    private void validateDraftQuoteRequest(DraftQuoteRequest request) {
        Assert.notNull(request, "DraftQuoteRequest must not be null");
        Assert.isTrue(providerType().equals(request.getProvider()), "Invalid provider for this operation");
        Assert.notNull(request.getDraftQuoteAmount(), "Draft quote amount must not be null");
        Assert.isTrue(request.getDraftQuoteAmount().compareTo(BigDecimal.ZERO) > 0, "Draft quote amount must be greater than zero");
        Assert.hasText(request.getClientName(), "Client name must not be blank");
        Assert.hasText(request.getClientPhone(), "Client phone must not be blank");
        Assert.hasText(request.getClientEmail(), "Client email must not be blank");
        Assert.hasText(request.getClientIdNumber(), "Client ID number must not be blank");
        Assert.notNull(request.getDraftQuoteUserId(), "Draft quote user ID must not be null");
        Assert.notNull(request.getInsuranceData(), "Insurance data must not be null");
        
        if (request.getInsuranceData().getVehicle() != null) {
            Assert.hasText(request.getInsuranceData().getVehicle().getRegistrationNumber(), "Vehicle registration number must not be blank");
        }
        
        if (request.getInsuranceData().getCover() != null) {
            Assert.notNull(request.getInsuranceData().getCover().getCoverStartDate(), "Cover start date must not be null");
            Assert.notNull(request.getInsuranceData().getCover().getCoverEndDate(), "Cover end date must not be null");
        }
    }

    private void validateGetDraftQuoteRequest(GetDraftQuoteRequest request) {
        Assert.notNull(request, "GetDraftQuoteRequest must not be null");
        Assert.isTrue(providerType().equals(request.getProvider()), "Invalid provider for this operation");
        Assert.notNull(request.getDraftQuoteSysId(), "Draft quote sys ID must not be null");
        Assert.isTrue(request.getDraftQuoteSysId() > 0, "Draft quote sys ID must be greater than zero");
    }
}
