package com.isec.platform.modules.integrations.mpesa.model;

import com.isec.platform.modules.integrations.mpesa.provider.MpesaProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpesaInitiatePaymentRequest {
    private MpesaProviderType partner;
    private String quoteRef;
    private String phoneNumber;
    private Double amount;
    private String customerReference;
    private Map<String, Object> metadata;
}
