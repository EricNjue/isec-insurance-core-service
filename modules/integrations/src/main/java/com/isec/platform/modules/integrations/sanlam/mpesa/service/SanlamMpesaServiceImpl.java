package com.isec.platform.modules.integrations.sanlam.mpesa.service;

import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.modules.integrations.sanlam.mpesa.client.SanlamMpesaClient;
import com.isec.platform.modules.integrations.sanlam.mpesa.dto.request.SanlamStkPushRequest;
import com.isec.platform.modules.integrations.sanlam.mpesa.dto.request.SanlamStkStatusRequest;
import com.isec.platform.modules.integrations.sanlam.mpesa.dto.response.SanlamStkPushResponse;
import com.isec.platform.modules.integrations.sanlam.mpesa.dto.response.SanlamStkStatusResponse;
import com.isec.platform.modules.integrations.sanlam.mpesa.model.MpesaPaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class SanlamMpesaServiceImpl implements SanlamMpesaService {

    private final SanlamMpesaClient mpesaClient;

    @Override
    public Mono<SanlamStkPushResponse> initiateStkPush(String quoteRef, String phoneNumber, Double amount) {
        validateStkPushRequest(quoteRef, phoneNumber, amount);

        SanlamStkPushRequest request = SanlamStkPushRequest.builder()
                .quoteRef(quoteRef)
                .phoneNumber(phoneNumber)
                .amount(amount)
                .build();

        return mpesaClient.initiateStkPush(request);
    }

    @Override
    public Mono<MpesaPaymentStatus> checkStatus(String quoteRef, String checkoutId) {
        Assert.notNull(quoteRef, "quote_ref must not be null");
        Assert.notNull(checkoutId, "checkout_id must not be null");

        SanlamStkStatusRequest request = SanlamStkStatusRequest.builder()
                .quoteRef(quoteRef)
                .checkoutId(checkoutId)
                .build();

        return mpesaClient.checkStkStatus(request)
                .map(this::mapToInternalModel);
    }

    private void validateStkPushRequest(String quoteRef, String phoneNumber, Double amount) {
        Assert.notNull(quoteRef, "quote_ref must not be null");
        Assert.notNull(phoneNumber, "phone_number must not be null");
        Assert.isTrue(amount != null && amount > 0, "amount must be greater than 0");
        
        // Basic phone number validation: starts with 254 and has 12 digits total
        if (!phoneNumber.matches("^254[0-9]{9}$")) {
            log.warn("Invalid phone number format: {}. Expected 254XXXXXXXXX", phoneNumber);
            throw new BusinessException("Invalid phone number format. Expected 254XXXXXXXXX");
        }
    }

    private MpesaPaymentStatus mapToInternalModel(SanlamStkStatusResponse response) {
        MpesaPaymentStatus.PaymentStatus status = MpesaPaymentStatus.PaymentStatus.FAILED;
        
        String externalStatus = response.getStatus();
        String errorCode = response.getRaw() != null ? response.getRaw().getErrorCode() : null;

        if ("success".equalsIgnoreCase(externalStatus)) {
            status = MpesaPaymentStatus.PaymentStatus.SUCCESS;
        } else if ("4999".equals(errorCode)) {
            status = MpesaPaymentStatus.PaymentStatus.PENDING;
        } else if ("1037".equals(errorCode) || "1032".equals(errorCode)) {
            status = MpesaPaymentStatus.PaymentStatus.FAILED;
        }

        return MpesaPaymentStatus.builder()
                .status(status)
                .message(response.getMessage())
                .receiptNumber(response.getReceipt())
                .amount(response.getAmount())
                .paidAt(response.getPaidAt())
                .rawResponse(response.getRaw())
                .build();
    }
}
