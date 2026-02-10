package com.isec.platform.modules.vehicles.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVehicleDto {
    private UUID id;
    private String registrationNumber;
    private String vehicleMake;
    private String vehicleModel;
    private Integer yearOfManufacture;
    private BigDecimal vehicleValue;
    private String chassisNumber;
    private String engineNumber;
}
