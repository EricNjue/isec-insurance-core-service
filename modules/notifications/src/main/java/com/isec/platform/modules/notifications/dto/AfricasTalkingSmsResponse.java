package com.isec.platform.modules.notifications.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfricasTalkingSmsResponse {
    @JsonProperty("SMSMessageData")
    private SMSMessageData SMSMessageData;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SMSMessageData {
        @JsonProperty("Message")
        private String Message;
        @JsonProperty("Recipients")
        private List<Recipient> Recipients;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recipient {
        private String number;
        private String cost;
        private String status;
        private int statusCode;
        private String messageId;
    }
}
