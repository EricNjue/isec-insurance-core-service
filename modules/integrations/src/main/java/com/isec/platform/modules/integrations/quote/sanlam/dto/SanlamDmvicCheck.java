package com.isec.platform.modules.integrations.quote.sanlam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanlamDmvicCheck {
    @JsonProperty("checked_at")
    private LocalDateTime checkedAt;
    
    @JsonProperty("has_double_insurance")
    private boolean hasDoubleInsurance;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("transaction_ref")
    private String transactionRef;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("evidence")
    private Map<String, Object> evidence;
}
