package com.isec.platform.modules.vehicles.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("vehicle_model")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleModel {

    @Id
    private UUID id;

    private UUID makeId;

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
