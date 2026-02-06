package com.isec.platform.modules.vehicles.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleModelRequest {

    @NotNull(message = "Make ID is required")
    private UUID makeId;

    @NotBlank(message = "Model code is required")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Model code must be uppercase and can contain numbers and underscores")
    private String code;

    @NotBlank(message = "Model name is required")
    private String name;

    private Integer yearFrom;
    private Integer yearTo;
    private String bodyType;
    private String fuelType;
    private Integer engineCapacityCc;

    @Builder.Default
    private boolean active = true;

    @AssertTrue(message = "year_from must be less than or equal to year_to")
    public boolean isYearRangeValid() {
        if (yearFrom == null || yearTo == null) {
            return true;
        }
        return yearFrom <= yearTo;
    }
}
