package com.isec.platform.modules.integrations.quote.provider;

import com.isec.platform.modules.integrations.mpesa.model.MpesaCheckStatusRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatusResponse;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationRequest;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationResponse;
import com.isec.platform.modules.integrations.quote.model.DraftQuoteRequest;
import com.isec.platform.modules.integrations.quote.model.DraftQuoteResponse;
import com.isec.platform.modules.integrations.quote.model.GetDraftQuoteRequest;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface PartnerQuoteProvider {

    PartnerType providerType();

    Set<QuoteLifecycleCapability> supportedCapabilities();

    Mono<PremiumCalculationResponse> calculatePremium(PremiumCalculationRequest request);

    default Mono<DraftQuoteResponse> createDraftQuote(DraftQuoteRequest request) {
        return Mono.error(new UnsupportedPartnerCapabilityException(
                providerType(), QuoteLifecycleCapability.CREATE_DRAFT_QUOTE
        ));
    }

    default Mono<DraftQuoteResponse> getDraftQuote(GetDraftQuoteRequest request) {
        return Mono.error(new UnsupportedPartnerCapabilityException(
                providerType(), QuoteLifecycleCapability.GET_DRAFT_QUOTE
        ));
    }

    default Mono<MpesaInitiatePaymentResponse> initiatePayment(MpesaInitiatePaymentRequest request) {
        return Mono.error(new UnsupportedPartnerCapabilityException(
                providerType(), QuoteLifecycleCapability.INITIATE_PAYMENT
        ));
    }

    default Mono<MpesaPaymentStatusResponse> checkPaymentStatus(MpesaCheckStatusRequest request) {
        return Mono.error(new UnsupportedPartnerCapabilityException(
                providerType(), QuoteLifecycleCapability.CHECK_PAYMENT_STATUS
        ));
    }
}
