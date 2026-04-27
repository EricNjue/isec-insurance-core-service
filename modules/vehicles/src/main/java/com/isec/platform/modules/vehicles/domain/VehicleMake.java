package com.isec.platform.modules.vehicles.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("vehicle_make")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleMake {

    @Id
    private UUID id;

    private String code;

    private String name;

    private String country;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
