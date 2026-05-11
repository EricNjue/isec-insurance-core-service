package com.isec.platform.modules.integrations.quote.sanlam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanlamUpdateDraftQuoteRequest {
    @JsonProperty("insurance_data")
    private InsuranceData insuranceData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsuranceData {
        @JsonProperty("payment")
        private PaymentData payment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentData {
        @JsonProperty("method")
        private String method;
        @JsonProperty("status")
        private String status;
        @JsonProperty("checkout_id")
        private String checkoutId;
        @JsonProperty("receipt")
        private String receipt;
        @JsonProperty("amount")
        private BigDecimal amount;
        @JsonProperty("paid_at")
        private String paidAt;
        @JsonProperty("phone_number")
        private String phoneNumber;
        @JsonProperty("installment_number")
        private Integer installmentNumber;
        @JsonProperty("numberOf_installments")
        private Integer numberOfInstallments;
    }
}
