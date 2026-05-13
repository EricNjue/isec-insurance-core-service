package com.isec.platform.modules.integrations.mpesa.provider;

import com.isec.platform.modules.integrations.mpesa.model.*;
import reactor.core.publisher.Mono;

public interface MpesaPaymentProvider {

    MpesaProviderType providerType();

    Mono<MpesaInitiatePaymentResponse> initiatePayment(MpesaInitiatePaymentRequest request);

    Mono<MpesaPaymentStatusResponse> checkStatus(MpesaCheckStatusRequest request);

    Mono<MpesaVerifyReceiptResponse> verifyReceipt(MpesaVerifyReceiptRequest request);

    default Mono<MpesaPaymentStatusResponse> verifyReceiptAndMap(MpesaVerifyReceiptRequest request) {
        return verifyReceipt(request)
                .map(res -> MpesaPaymentStatusResponse.builder()
                        .status("success".equalsIgnoreCase(res.getStatus()) ? MpesaPaymentStatus.SUCCESS : MpesaPaymentStatus.FAILED)
                        .message(res.getMessage())
                        .amount(res.getAmount() != null ? res.getAmount().doubleValue() : 0.0)
                        .paidAt(res.getPaidAt())
                        .receiptNumber(request.getReceipt())
                        .checkoutId("MANUAL_" + request.getReceipt())
                        .rawResponse(res.getRaw())
                        .build());
    }
}
