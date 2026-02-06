package com.isec.platform.modules.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsSendResult {
    private String summaryMessage;
    private List<RecipientResult> recipients;
    private boolean overallSuccess;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipientResult {
        private String number;
        private String status;
        private int statusCode;
        private String messageId;
        private String cost;
    }
}
