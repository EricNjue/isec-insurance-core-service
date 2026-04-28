package com.isec.platform.modules.integrations.quote.model;

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
public class QuoteDmvicCheck {
    private LocalDateTime checkedAt;
    private boolean hasDoubleInsurance;
    private String status;
    private String transactionRef;
    private String message;
    private Map<String, Object> evidence;
}
