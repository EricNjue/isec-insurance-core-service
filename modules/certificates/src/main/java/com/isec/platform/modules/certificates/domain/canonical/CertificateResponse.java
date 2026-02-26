package com.isec.platform.modules.certificates.domain.canonical;

import java.time.Instant;

public record CertificateResponse(
        ProviderType providerType,
        CertificateStatus status,
        String certificateNumber,
        String externalReference,
        String message,
        Instant issuedAt
) {
}
