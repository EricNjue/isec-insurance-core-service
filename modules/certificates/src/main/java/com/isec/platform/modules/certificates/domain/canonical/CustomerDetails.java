package com.isec.platform.modules.certificates.domain.canonical;

import jakarta.validation.constraints.NotBlank;

public record CustomerDetails(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String otherNames,
        String email,
        String phoneNumber,
        String idNumber,
        String addressLine1,
        String addressLine2
) {
}
