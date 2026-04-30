package com.isec.platform.modules.integrations.mpesa.sanlam.service;

import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.modules.integrations.mpesa.model.MpesaCheckStatusRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatusResponse;
import com.isec.platform.modules.integrations.mpesa.provider.MpesaPaymentProvider;
import com.isec.platform.modules.integrations.mpesa.provider.MpesaProviderType;
import com.isec.platform.modules.integrations.mpesa.sanlam.client.SanlamMpesaClient;
import com.isec.platform.modules.integrations.mpesa.sanlam.dto.SanlamStkPushRequest;
import com.isec.platform.modules.integrations.mpesa.sanlam.dto.SanlamStkStatusRequest;
import com.isec.platform.modules.integrations.mpesa.sanlam.mapper.SanlamMpesaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class SanlamMpesaProvider implements MpesaPaymentProvider {

    private final SanlamMpesaClient mpesaClient;
    private final SanlamMpesaMapper mapper;

    @Override
    public MpesaProviderType providerType() {
        return MpesaProviderType.SANLAM;
    }

    @Override
    public Mono<MpesaInitiatePaymentResponse> initiatePayment(MpesaInitiatePaymentRequest request) {
        validateInitiateRequest(request);

        SanlamStkPushRequest sanlamRequest = mapper.toSanlamStkPushRequest(request);

        log.info("[{}] Initiating payment for quoteRef: {}, phone: {}",
                providerType(), request.getQuoteRef(), maskPhoneNumber(request.getPhoneNumber()));

        return mpesaClient.initiateStkPush(sanlamRequest)
                .map(mapper::toMpesaInitiatePaymentResponse)
                .doOnNext(res -> {
                    if (res.getStatus() == com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatus.FAILED || res.getCheckoutId() == null) {
                        String errorMsg = String.format("[SANLAM] Payment initiated for quoteRef: %s, checkoutId: %s, status: %s",
                                request.getQuoteRef(), res.getCheckoutId(), res.getStatus());
                        log.error(errorMsg);
                        throw new BusinessException("We could not initiate the payment. Please try again later.");
                    }
                    log.info("[{}] Payment initiated for quoteRef: {}, checkoutId: {}, status: {}",
                            providerType(), request.getQuoteRef(), res.getCheckoutId(), res.getStatus());
                });
    }

    @Override
    public Mono<MpesaPaymentStatusResponse> checkStatus(MpesaCheckStatusRequest request) {
        Assert.notNull(request.getQuoteRef(), "quoteRef must not be null");
        Assert.notNull(request.getCheckoutId(), "checkoutId must not be null");

        SanlamStkStatusRequest sanlamRequest = mapper.toSanlamStkStatusRequest(request.getQuoteRef(), request.getCheckoutId());

        log.info("[{}] Checking status for quoteRef: {}, checkoutId: {}",
                providerType(), request.getQuoteRef(), request.getCheckoutId());

        return mpesaClient.checkStkStatus(sanlamRequest)
                .map(res -> mapper.toMpesaPaymentStatusResponse(res, request.getCheckoutId()))
                .doOnNext(res -> log.info("[{}] Status check for quoteRef: {}, checkoutId: {}, status: {}",
                        providerType(), request.getQuoteRef(), request.getCheckoutId(), res.getStatus()));
    }

    private void validateInitiateRequest(MpesaInitiatePaymentRequest request) {
        Assert.notNull(request.getQuoteRef(), "quoteRef must not be null");
        Assert.notNull(request.getPhoneNumber(), "phoneNumber must not be null");
        Assert.isTrue(request.getAmount() != null && request.getAmount() > 0, "amount must be greater than 0");

        if (!request.getPhoneNumber().matches("^254[0-9]{9}$")) {
            log.warn("[{}] Invalid phone number format: {}", providerType(), maskPhoneNumber(request.getPhoneNumber()));
            throw new BusinessException("Invalid phone number format. Expected 254XXXXXXXXX");
        }
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 7) return "****";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }
}
