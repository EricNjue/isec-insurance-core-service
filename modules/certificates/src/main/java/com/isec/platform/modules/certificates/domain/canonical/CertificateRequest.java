package com.isec.platform.modules.certificates.domain.canonical;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CertificateRequest(
        @NotBlank String idempotencyKey,
        @NotNull CertificateType certificateType,
        ProviderType providerType,
        @NotNull @Valid PolicyDetails policyDetails,
        @NotNull @Valid CustomerDetails customerDetails,
        @NotNull @Valid VehicleDetails vehicleDetails,
        @NotNull @Valid Money premium
) {
}
