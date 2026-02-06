package com.isec.platform.modules.vehicles.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleModelResponse {
    private UUID id;
    private UUID makeId;
    private String makeCode;
    private String makeName;
    private String code;
    private String name;
    private Integer yearFrom;
    private Integer yearTo;
    private String bodyType;
    private String fuelType;
    private Integer engineCapacityCc;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
