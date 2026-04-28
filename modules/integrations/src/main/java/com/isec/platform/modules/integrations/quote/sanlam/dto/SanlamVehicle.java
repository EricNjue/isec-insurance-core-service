package com.isec.platform.modules.integrations.quote.sanlam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanlamVehicle {
    @JsonProperty("make")
    private String make;
    
    @JsonProperty("model")
    private String model;
    
    @JsonProperty("year_of_manufacture")
    private Integer yearOfManufacture;
    
    @JsonProperty("sum_insured")
    private BigDecimal sumInsured;
    
    @JsonProperty("registration_number")
    private String registrationNumber;
    
    @JsonProperty("registration")
    private String registration;
    
    @JsonProperty("year")
    private Integer year;
    
    @JsonProperty("value")
    private BigDecimal value;
    
    @JsonProperty("body_type")
    private String bodyType;
    
    @JsonProperty("chassis_number")
    private String chassisNumber;
    
    @JsonProperty("engine_number")
    private String engineNumber;
    
    @JsonProperty("seating_capacity")
    private String seatingCapacity;
    
    @JsonProperty("tonnage")
    private String tonnage;
    
    @JsonProperty("number_of_passengers")
    private String numberOfPassengers;
    
    @JsonProperty("cc")
    private String cc;
    
    @JsonProperty("motor_class")
    private String motorClass;
    
    @JsonProperty("vehicle_class")
    private String vehicleClass;
}
