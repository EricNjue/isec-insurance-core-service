package com.isec.platform.modules.integrations.mpesa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class MpesaDtos {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OAuthResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("expires_in")
        private String expiresIn;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StkPushRequest {
        @JsonProperty("BusinessShortCode")
        private String businessShortCode;
        @JsonProperty("Password")
        private String password;
        @JsonProperty("Timestamp")
        private String timestamp;
        @JsonProperty("TransactionType")
        private String transactionType;
        @JsonProperty("Amount")
        private Integer amount;
        @JsonProperty("PartyA")
        private String partyA;
        @JsonProperty("PartyB")
        private String partyB;
        @JsonProperty("PhoneNumber")
        private String phoneNumber;
        @JsonProperty("CallBackURL")
        private String callBackUrl;
        @JsonProperty("AccountReference")
        private String accountReference;
        @JsonProperty("TransactionDesc")
        private String transactionDesc;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StkPushResponse {
        @JsonProperty("MerchantRequestID")
        private String merchantRequestID;
        @JsonProperty("CheckoutRequestID")
        private String checkoutRequestID;
        @JsonProperty("ResponseCode")
        private String responseCode;
        @JsonProperty("ResponseDescription")
        private String responseDescription;
        @JsonProperty("CustomerMessage")
        private String customerMessage;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StkQueryRequest {
        @JsonProperty("BusinessShortCode")
        private String businessShortCode;
        @JsonProperty("Password")
        private String password;
        @JsonProperty("Timestamp")
        private String timestamp;
        @JsonProperty("CheckoutRequestID")
        private String checkoutRequestID;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StkQueryResponse {
        @JsonProperty("ResponseCode")
        private String responseCode;
        @JsonProperty("ResponseDescription")
        private String responseDescription;
        @JsonProperty("MerchantRequestID")
        private String merchantRequestID;
        @JsonProperty("CheckoutRequestID")
        private String checkoutRequestID;
        @JsonProperty("ResultCode")
        private String resultCode;
        @JsonProperty("ResultDesc")
        private String resultDesc;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallbackRequest {
        @JsonProperty("Body")
        private CallbackBody body;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallbackBody {
        @JsonProperty("stkCallback")
        private StkCallback stkCallback;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StkCallback {
        @JsonProperty("MerchantRequestID")
        private String merchantRequestID;
        @JsonProperty("CheckoutRequestID")
        private String checkoutRequestID;
        @JsonProperty("ResultCode")
        private Integer resultCode;
        @JsonProperty("ResultDesc")
        private String resultDesc;
        @JsonProperty("CallbackMetadata")
        private CallbackMetadata callbackMetadata;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallbackMetadata {
        @JsonProperty("Item")
        private List<Item> item;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Value")
        private Object value;
    }
}
