package com.isec.platform.modules.applications.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoubleInsuranceRequest {
    @NotBlank(message = "License plate number is required")
    private String licensePlateNumber;

    private String chassisNumber;
}
