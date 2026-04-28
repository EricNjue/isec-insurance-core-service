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
public class MpesaCheckStatusRequest {
    private MpesaProviderType partner;
    private String quoteRef;
    private String checkoutId;
    private Map<String, Object> metadata;
}
