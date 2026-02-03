package com.isec.platform.modules.applications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ApplicationRequest {
    @NotBlank
    private String registrationNumber;
    @NotBlank
    private String vehicleMake;
    @NotBlank
    private String vehicleModel;
    @NotNull
    @Positive
    private Integer yearOfManufacture;
    @NotNull
    @Positive
    private BigDecimal vehicleValue;
}
