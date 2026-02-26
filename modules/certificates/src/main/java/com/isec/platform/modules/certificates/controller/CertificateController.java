package com.isec.platform.modules.certificates.controller;

import com.isec.platform.modules.certificates.application.CertificateApplicationService;
import com.isec.platform.modules.certificates.domain.canonical.CertificateRequest;
import com.isec.platform.modules.certificates.domain.canonical.CertificateResponse;
import com.isec.platform.modules.certificates.domain.canonical.ProviderType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Slf4j
public class CertificateController {

    private final CertificateApplicationService certificateApplicationService;

    @PostMapping("/issue")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<CertificateResponse> issueCertificate(@Valid @RequestBody CertificateRequest request) {
        log.info("Certificate issuance request received for policy {}", request.policyDetails().policyNumber());
        return ResponseEntity.ok(certificateApplicationService.issueCertificate(request));
    }

    @GetMapping("/status/{externalReference}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<CertificateResponse> checkStatus(@PathVariable String externalReference,
                                                           @RequestParam(name = "provider", required = false) ProviderType providerType) {
        log.debug("Certificate status check for external reference {}", externalReference);
        return ResponseEntity.ok(certificateApplicationService.checkStatus(externalReference, providerType));
    }
}
