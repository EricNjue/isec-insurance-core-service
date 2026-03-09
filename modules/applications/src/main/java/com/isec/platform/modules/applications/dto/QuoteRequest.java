package com.isec.platform.modules.applications.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteRequest {
    private String quoteId;
    @Valid
    @NotNull
    private InsuranceDetails insuranceDetails;
    @Valid
    @NotNull
    private VehicleDetails vehicleDetails;
    @Valid
    private KycDetails kycDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsuranceDetails {
        @NotBlank
        private String category; // PRIVATE_CAR
        private String insuranceType; // General Insurance | Medical Insurance | Life Insurance
        private String coverType; // Comprehensive Cover | Third-Party Cover
        private String insuranceCompany; // APA | Sanlam | Mayfair | iSec
        private String insuranceStartDate; // 2026/12/20
        private String insuranceDuration; // MONTH_1 | MONTH_2 | ANNUAL
        private List<Long> addonRuleIds;
        private Map<String, Object> additionalData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleDetails {
        @NotNull
        @Positive
        private BigDecimal valuationAmount;
        private String valuationCurrency; // KES
        @NotBlank
        private String licensePlateNumber;
        @NotNull
        private Integer yearOfManufacture;
        @NotBlank
        private String makeCode;
        @NotBlank
        private String modelCode;
        private String engineCapacity;
        private String chassisNumber;
        private String vinNumber;
        private String engineNumber;
        private String usageType; // Commercial | Personal | Pay as you drive (PAYD)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KycDetails {
        private String email;
        private String fullName;
        private String phoneNumber;
        private String physicalAddress;
    }
}
