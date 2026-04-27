package com.isec.platform.modules.certificates.service;

import com.isec.platform.modules.certificates.domain.Certificate;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import com.isec.platform.modules.documents.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CertificateRetrievalService {

    private final CertificateRepository certificateRepository;
    private final S3Service s3Service;

    public Mono<Certificate> getCertificateMetadata(String certificateNumber) {
        return certificateRepository.findByPartnerCodeAndCertificateNumber(null, certificateNumber)
                .switchIfEmpty(certificateRepository.findByPolicyNumber(certificateNumber)) // Fallback to policy number if cert not found
                .switchIfEmpty(Mono.error(new NoSuchElementException("Certificate not found: " + certificateNumber)));
    }

    public Mono<String> generateDownloadUrl(String certificateNumber) {
        return getCertificateMetadata(certificateNumber)
                .flatMap(cert -> {
                    if (cert.getS3Key() == null) {
                        return Mono.error(new IllegalStateException("Certificate document not yet available in storage"));
                    }
                    return Mono.just(s3Service.generatePresignedGetUrl(cert.getS3Key()));
                });
    }
}
