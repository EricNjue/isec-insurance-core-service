package com.isec.platform.modules.certificates.domain.canonical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PolicyDetails(
        @NotBlank String policyNumber,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String productType
) {
}
