package com.isec.platform.modules.integrations.mpesa.sanlam.mapper;

import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatus;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatusResponse;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse;
import com.isec.platform.modules.integrations.mpesa.provider.MpesaProviderType;
import com.isec.platform.modules.integrations.mpesa.sanlam.dto.SanlamStkPushRequest;
import com.isec.platform.modules.integrations.mpesa.sanlam.dto.SanlamStkPushResponse;
import com.isec.platform.modules.integrations.mpesa.sanlam.dto.SanlamStkStatusRequest;
import com.isec.platform.modules.integrations.mpesa.sanlam.dto.SanlamStkStatusResponse;
import org.springframework.stereotype.Component;

@Component
public class SanlamMpesaMapper {

    public SanlamStkPushRequest toSanlamStkPushRequest(MpesaInitiatePaymentRequest request) {
        return SanlamStkPushRequest.builder()
                .quoteRef(request.getQuoteRef())
                .phoneNumber(request.getPhoneNumber())
                .amount(request.getAmount())
                .build();
    }

    public MpesaInitiatePaymentResponse toMpesaInitiatePaymentResponse(SanlamStkPushResponse response) {
        return MpesaInitiatePaymentResponse.builder()
                .provider(MpesaProviderType.SANLAM)
                .status("accepted".equalsIgnoreCase(response.getStatus()) ? MpesaPaymentStatus.ACCEPTED : MpesaPaymentStatus.FAILED)
                .message(response.getMessage())
                .checkoutId(response.getCheckoutId())
                .providerReference(response.getCheckoutId())
                .rawResponse(response)
                .build();
    }

    public SanlamStkStatusRequest toSanlamStkStatusRequest(String quoteRef, String checkoutId) {
        return SanlamStkStatusRequest.builder()
                .quoteRef(quoteRef)
                .checkoutId(checkoutId)
                .build();
    }

    public MpesaPaymentStatusResponse toMpesaPaymentStatusResponse(SanlamStkStatusResponse response, String checkoutId) {
        return MpesaPaymentStatusResponse.builder()
                .provider(MpesaProviderType.SANLAM)
                .status(mapStatus(response))
                .message(response.getMessage())
                .receiptNumber(response.getReceipt())
                .amount(response.getAmount())
                .paidAt(response.getPaidAt())
                .checkoutId(checkoutId)
                .rawResponse(response.getRaw())
                .build();
    }

    private MpesaPaymentStatus mapStatus(SanlamStkStatusResponse response) {
        String externalStatus = response.getStatus();
        String errorCode = response.getRaw() != null ? response.getRaw().getErrorCode() : null;

        if ("success".equalsIgnoreCase(externalStatus)) {
            return MpesaPaymentStatus.SUCCESS;
        }

        if (errorCode == null) {
            return MpesaPaymentStatus.UNKNOWN;
        }

        return switch (errorCode) {
            case "4999" -> MpesaPaymentStatus.PENDING;
            case "1037" -> MpesaPaymentStatus.TIMEOUT;
            case "1032" -> MpesaPaymentStatus.CANCELLED;
            default -> MpesaPaymentStatus.FAILED;
        };
    }
}
