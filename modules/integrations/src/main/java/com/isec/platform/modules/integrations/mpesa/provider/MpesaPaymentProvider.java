package com.isec.platform.modules.integrations.mpesa.provider;

import com.isec.platform.modules.integrations.mpesa.model.MpesaCheckStatusRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatusResponse;
import reactor.core.publisher.Mono;

public interface MpesaPaymentProvider {

    MpesaProviderType providerType();

    Mono<MpesaInitiatePaymentResponse> initiatePayment(MpesaInitiatePaymentRequest request);

    Mono<MpesaPaymentStatusResponse> checkStatus(MpesaCheckStatusRequest request);
}
