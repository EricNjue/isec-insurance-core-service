package com.isec.platform.modules.integrations.mpesa.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MpesaVerifyReceiptResponse {
    private String status;
    private String message;
    private BigDecimal amount;
    @JsonProperty("paid_at")
    private String paidAt;
    private Map<String, Object> raw;
    
    // Additional fields from failure response to be safe
    @JsonProperty("account_number_mismatch")
    private Object accountNumberMismatch;
    @JsonProperty("account_number_used")
    private Object accountNumberUsed;
    @JsonProperty("expected_registration")
    private Object expectedRegistration;
    @JsonProperty("receipt_expired")
    private Object receiptExpired;
    @JsonProperty("receipt_age_days")
    private Object receiptAgeDays;
}
