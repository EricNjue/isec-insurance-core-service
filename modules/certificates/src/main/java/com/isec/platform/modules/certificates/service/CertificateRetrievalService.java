package com.isec.platform.modules.certificates.service;

import com.isec.platform.modules.certificates.domain.Certificate;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import com.isec.platform.modules.documents.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CertificateRetrievalService {

    private final CertificateRepository certificateRepository;
    private final S3Service s3Service;

    public Certificate getCertificateMetadata(String certificateNumber) {
        return certificateRepository.findByPartnerCodeAndCertificateNumber(null, certificateNumber)
                .or(() -> certificateRepository.findByPolicyNumber(certificateNumber)) // Fallback to policy number if cert not found
                .orElseThrow(() -> new NoSuchElementException("Certificate not found: " + certificateNumber));
    }

    public String generateDownloadUrl(String certificateNumber) {
        Certificate cert = getCertificateMetadata(certificateNumber);
        if (cert.getS3Key() == null) {
            throw new IllegalStateException("Certificate document not yet available in storage");
        }
        return s3Service.generatePresignedGetUrl(cert.getS3Key());
    }
}
