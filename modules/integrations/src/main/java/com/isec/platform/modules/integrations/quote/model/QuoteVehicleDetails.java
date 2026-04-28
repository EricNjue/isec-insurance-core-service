package com.isec.platform.modules.integrations.quote.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteVehicleDetails {
    private String make;
    private String model;
    private Integer yearOfManufacture;
    private BigDecimal sumInsured;
    private String registrationNumber;
    private String bodyType;
    private String chassisNumber;
    private String engineNumber;
    private String seatingCapacity;
    private String tonnage;
    private String cc;
    private String motorClass;
    private String vehicleClass;
}
