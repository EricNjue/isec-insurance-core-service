package com.isec.platform.modules.integrations.mpesa.model;

import com.isec.platform.modules.integrations.mpesa.provider.MpesaProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpesaInitiatePaymentResponse {
    private MpesaProviderType provider;
    private MpesaPaymentStatus status;
    private String message;
    private String checkoutId;
    private String providerReference;
    private Object rawResponse;
}
