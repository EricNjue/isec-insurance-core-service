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
        @JsonProperty("vehicle")
        private SanlamVehicle vehicle;
        @JsonProperty("benefits")
        private SanlamBenefits benefits;
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
        @JsonProperty("total_amount")
        private BigDecimal totalAmount;
        @JsonProperty("total_paid")
        private BigDecimal totalPaid;
        @JsonProperty("remaining_balance")
        private BigDecimal remainingBalance;
        @JsonProperty("installment_count")
        private Integer installmentCount;
        @JsonProperty("numberOf_installments")
        private Integer numberOfInstallments;
        @JsonProperty("max_installments")
        private Integer maxInstallments;
        @JsonProperty("installments")
        private List<InstallmentData> installments;
        @JsonProperty("payment_context")
        private String paymentContext;
        @JsonProperty("last_payment")
        private LastPaymentData lastPayment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallmentData {
        @JsonProperty("installment_number")
        private Integer installmentNumber;
        @JsonProperty("amount")
        private BigDecimal amount;
        @JsonProperty("receipt")
        private String receipt;
        @JsonProperty("paid_at")
        private String paidAt;
        @JsonProperty("method")
        private String method;
        @JsonProperty("checkout_id")
        private String checkoutId;
        @JsonProperty("phone_number")
        private String phoneNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastPaymentData {
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
    }
}
