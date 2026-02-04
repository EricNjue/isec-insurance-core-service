package com.isec.platform.modules.certificates.controller;

import com.isec.platform.modules.certificates.domain.Certificate;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import com.isec.platform.modules.certificates.service.CertificateService;
import com.isec.platform.modules.policies.domain.Policy;
import com.isec.platform.modules.policies.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Slf4j
public class CertificateController {

    private final CertificateService certificateService;
    private final CertificateRepository certificateRepository;
    private final PolicyRepository policyRepository;

    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<Void> requestIssuance(@RequestParam Long policyId, @RequestParam BigDecimal amount) {
        log.info("Manual certificate issuance requested. policyId={}, amount={}", policyId, amount);
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found"));
        
        certificateService.processCertificateIssuance(policy, amount);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/policy/{policyId}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<List<Certificate>> getCertificates(@PathVariable Long policyId) {
        log.debug("Fetching certificates for policyId={}", policyId);
        return ResponseEntity.ok(certificateRepository.findByPolicyId(policyId));
    }
}
