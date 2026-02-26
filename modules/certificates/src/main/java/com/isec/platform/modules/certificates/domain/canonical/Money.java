package com.isec.platform.modules.certificates.domain.canonical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record Money(
        @NotNull BigDecimal amount,
        @NotBlank String currency
) {
}
