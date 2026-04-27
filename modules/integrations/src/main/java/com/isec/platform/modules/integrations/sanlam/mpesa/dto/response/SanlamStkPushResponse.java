package com.isec.platform.modules.integrations.sanlam.mpesa.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanlamStkPushResponse {
    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("checkout_id")
    private String checkoutId;

    @JsonProperty("raw")
    private RawResponse raw;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RawResponse {
        @JsonProperty("MerchantRequestID")
        private String merchantRequestId;

        @JsonProperty("CheckoutRequestID")
        private String checkoutRequestId;

        @JsonProperty("ResponseCode")
        private String responseCode;

        @JsonProperty("ResponseDescription")
        private String responseDescription;

        @JsonProperty("CustomerMessage")
        private String customerMessage;
    }
}
