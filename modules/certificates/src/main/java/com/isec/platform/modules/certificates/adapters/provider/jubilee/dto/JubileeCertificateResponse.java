package com.isec.platform.modules.certificates.adapters.provider.jubilee.dto;

public record JubileeCertificateResponse(
        String certificateNumber,
        String externalReference,
        String status,
        String message
) {
}
