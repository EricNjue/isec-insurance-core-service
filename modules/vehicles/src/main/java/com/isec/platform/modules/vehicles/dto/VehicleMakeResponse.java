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
public class VehicleMakeResponse {
    private UUID id;
    private String code;
    private String name;
    private String country;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
