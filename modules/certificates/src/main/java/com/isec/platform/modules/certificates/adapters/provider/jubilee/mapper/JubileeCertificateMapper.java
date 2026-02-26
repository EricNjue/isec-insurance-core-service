package com.isec.platform.modules.certificates.adapters.provider.jubilee.mapper;

import com.isec.platform.modules.certificates.adapters.provider.jubilee.dto.JubileeCertificateRequest;
import com.isec.platform.modules.certificates.adapters.provider.jubilee.dto.JubileeCertificateResponse;
import com.isec.platform.modules.certificates.adapters.provider.jubilee.dto.JubileeStatusResponse;
import com.isec.platform.modules.certificates.domain.canonical.CertificateRequest;
import com.isec.platform.modules.certificates.domain.canonical.CertificateResponse;
import com.isec.platform.modules.certificates.domain.canonical.CertificateStatus;
import com.isec.platform.modules.certificates.domain.canonical.ProviderType;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JubileeCertificateMapper {

    public JubileeCertificateRequest toProviderRequest(CertificateRequest request) {
        String insuredName = request.customerDetails().firstName() + " " + request.customerDetails().lastName();
        return new JubileeCertificateRequest(
                request.policyDetails().policyNumber(),
                request.vehicleDetails().registrationNumber(),
                insuredName,
                request.premium().amount(),
                request.premium().currency(),
                request.policyDetails().startDate().toString(),
                request.policyDetails().endDate().toString()
        );
    }

    public CertificateResponse toCanonicalResponse(JubileeCertificateResponse response) {
        return new CertificateResponse(
                ProviderType.JUBILEE,
                mapStatus(response.status()),
                response.certificateNumber(),
                response.externalReference(),
                response.message(),
                Instant.now()
        );
    }

    public CertificateResponse toCanonicalResponse(JubileeStatusResponse response) {
        return new CertificateResponse(
                ProviderType.JUBILEE,
                mapStatus(response.status()),
                response.certificateNumber(),
                response.externalReference(),
                response.message(),
                Instant.now()
        );
    }

    private CertificateStatus mapStatus(String status) {
        if (status == null) {
            return CertificateStatus.PENDING;
        }
        return switch (status.toUpperCase()) {
            case "ISSUED" -> CertificateStatus.ISSUED;
            case "FAILED" -> CertificateStatus.FAILED;
            case "PROCESSING" -> CertificateStatus.PROCESSING;
            default -> CertificateStatus.PENDING;
        };
    }
}
