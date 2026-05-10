package com.isec.platform.modules.integrations.quote.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyIssuanceResult {
    private String status;
    private String message;
    private String policyReference;
    private String externalReference;
    private boolean emailSent;
    private Map<String, Object> metadata;
    private String rawResponse;
}
