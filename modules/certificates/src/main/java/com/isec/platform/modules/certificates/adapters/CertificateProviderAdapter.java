package com.isec.platform.modules.certificates.adapters;

import com.isec.platform.modules.certificates.domain.canonical.CertificateRequest;
import com.isec.platform.modules.certificates.domain.canonical.CertificateResponse;
import com.isec.platform.modules.certificates.domain.canonical.ProviderType;

public interface CertificateProviderAdapter {

    ProviderType providerType();

    CertificateResponse issueCertificate(CertificateRequest request);

    CertificateResponse checkStatus(String externalReference);
}
