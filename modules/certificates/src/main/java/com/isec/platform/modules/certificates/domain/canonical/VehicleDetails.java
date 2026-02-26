package com.isec.platform.modules.certificates.domain.canonical;

import jakarta.validation.constraints.NotBlank;

public record VehicleDetails(
        @NotBlank String registrationNumber,
        @NotBlank String make,
        @NotBlank String model,
        String chassisNumber,
        String engineNumber,
        String bodyType
) {
}
