package com.isec.platform.modules.integrations.sanlam.mpesa.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanlamStkStatusResponse {
    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("receipt")
    private String receipt;

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("paid_at")
    private String paidAt;

    @JsonProperty("raw")
    private RawResponse raw;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RawResponse {
        @JsonProperty("status")
        private boolean status;

        @JsonProperty("message")
        private String message;

        @JsonProperty("receipt")
        private Object receipt; // Can be a list or an object

        @JsonProperty("error")
        private Object error; // Can be a list or an object

        @JsonProperty("error_code")
        private String errorCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Receipt {
        @JsonProperty("number")
        private String number;

        @JsonProperty("transaction_date")
        private String transactionDate;

        @JsonProperty("bill_reference_number")
        private String billReferenceNumber;

        @JsonProperty("amount")
        private String amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Error {
        @JsonProperty("errorId")
        private String errorId;

        @JsonProperty("errorCode")
        private String errorCode;

        @JsonProperty("errorMessage")
        private String errorMessage;
    }
}
