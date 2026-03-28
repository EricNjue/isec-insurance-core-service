package com.isec.platform.modules.integrations.sanlam.dto;

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
public class SanlamDoubleInsuranceResponse {
    private String status;
    private String message;
    @JsonProperty("transaction_ref")
    private String transactionRef;
    private Evidence evidence;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Evidence {
        @JsonProperty("api_request_number")
        private String apiRequestNumber;
        private Boolean status;
        private String error;
        @JsonProperty("double_insurance")
        private List<Object> doubleInsurance;

        // Fields for duplicate found
        @JsonProperty("cover_end_date")
        private String coverEndDate;
        @JsonProperty("certificate_number")
        private String certificateNumber;
        @JsonProperty("policy_number")
        private String policyNumber;
        private String insurer;
        @JsonProperty("vehicle_registration_number")
        private String vehicleRegistrationNumber;
        @JsonProperty("vehicle_chassis_number")
        private String vehicleChassisNumber;
        @JsonProperty("certificate_status")
        private String certificateStatus;
    }
}
