package com.isec.platform.modules.integrations.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoubleInsuranceCheckResponse {
    private boolean hasDuplicate;
    private String status; // e.g., "clear", "double"
    private String message;
    private DuplicateDetails details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DuplicateDetails {
        private String insurer;
        private String policyNumber;
        private String certificateNumber;
        private String expiryDate;
        private String status;
    }
}
