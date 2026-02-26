package com.isec.platform.modules.certificates.adapters.provider.jubilee.dto;

import java.math.BigDecimal;

public record JubileeCertificateRequest(
        String policyNumber,
        String registrationNumber,
        String insuredName,
        BigDecimal premiumAmount,
        String currency,
        String startDate,
        String endDate
) {
}
