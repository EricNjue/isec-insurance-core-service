package com.isec.platform.modules.integrations.sanlam.mpesa.service;

import com.isec.platform.modules.integrations.sanlam.mpesa.dto.response.SanlamStkPushResponse;
import com.isec.platform.modules.integrations.sanlam.mpesa.model.MpesaPaymentStatus;
import reactor.core.publisher.Mono;

public interface SanlamMpesaService {
    Mono<SanlamStkPushResponse> initiateStkPush(String quoteRef, String phoneNumber, Double amount);
    Mono<MpesaPaymentStatus> checkStatus(String quoteRef, String checkoutId);
}
