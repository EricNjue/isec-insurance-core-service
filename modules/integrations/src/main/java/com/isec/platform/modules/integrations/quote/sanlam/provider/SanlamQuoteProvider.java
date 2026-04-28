package com.isec.platform.modules.integrations.quote.sanlam.provider;

import com.isec.platform.modules.integrations.mpesa.model.MpesaCheckStatusRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatusResponse;
import com.isec.platform.modules.integrations.mpesa.sanlam.service.SanlamMpesaProvider;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationRequest;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationResponse;
import com.isec.platform.modules.integrations.premium.sanlam.provider.SanlamPremiumCalculationProvider;
import com.isec.platform.modules.integrations.quote.model.DraftQuoteRequest;
import com.isec.platform.modules.integrations.quote.model.DraftQuoteResponse;
import com.isec.platform.modules.integrations.quote.model.GetDraftQuoteRequest;
import com.isec.platform.modules.integrations.quote.provider.PartnerQuoteProvider;
import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import com.isec.platform.modules.integrations.quote.provider.QuoteLifecycleCapability;
import com.isec.platform.modules.integrations.quote.sanlam.client.SanlamDraftQuoteClient;
import com.isec.platform.modules.integrations.quote.sanlam.mapper.SanlamDraftQuoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class SanlamQuoteProvider implements PartnerQuoteProvider {

    private final SanlamDraftQuoteClient client;
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
                QuoteLifecycleCapability.CHECK_PAYMENT_STATUS
        );
    }

    @Override
    public Mono<PremiumCalculationResponse> calculatePremium(PremiumCalculationRequest request) {
        return premiumProvider.calculatePremium(request);
    }

    @Override
    public Mono<DraftQuoteResponse> createDraftQuote(DraftQuoteRequest request) {
        return Mono.fromRunnable(() -> validateDraftQuoteRequest(request))
                .then(Mono.defer(() -> {
                    long startTime = Instant.now().toEpochMilli();
                    log.info("Starting Sanlam draft quote creation for client: {}", request.getClientName());

                    return client.createDraftQuote(mapper.toSanlamRequest(request))
                            .map(mapper::toCommonResponse)
                            .doOnNext(response -> {
                                long latency = Instant.now().toEpochMilli() - startTime;
                                log.info("Sanlam draft quote creation completed. Ref: {}, Latency: {}ms",
                                        response.getDraftQuoteRef(), latency);
                            })
                            .doOnError(error -> log.error("Sanlam draft quote creation failed. Error: {}", error.getMessage()));
                }));
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
