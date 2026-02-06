package com.isec.platform.modules.vehicles.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleMakeRequest {

    @NotBlank(message = "Make code is required")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Make code must be uppercase and can contain numbers and underscores")
    private String code;

    @NotBlank(message = "Make name is required")
    private String name;

    private String country;

    @Builder.Default
    private boolean active = true;
}
