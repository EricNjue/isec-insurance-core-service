package com.isec.platform.modules.certificates.controller;

import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.certificates.domain.Certificate;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import com.isec.platform.modules.certificates.service.CertificateResendService;
import com.isec.platform.modules.certificates.service.CertificateRetrievalService;
import com.isec.platform.modules.certificates.service.CertificateService;
import com.isec.platform.modules.customers.domain.Customer;
import com.isec.platform.modules.customers.service.CustomerService;
import com.isec.platform.modules.policies.domain.Policy;
import com.isec.platform.modules.policies.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Slf4j
public class CertificateController {

    private final CertificateService certificateService;
    private final CertificateRepository certificateRepository;
    private final PolicyRepository policyRepository;
    private final ApplicationRepository applicationRepository;
    private final CustomerService customerService;
    private final CertificateRetrievalService retrievalService;
    private final CertificateResendService resendService;

    @GetMapping("/{certificateNumber}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<Certificate> getCertificateMetadata(@PathVariable String certificateNumber) {
        log.debug("Fetching certificate metadata for {}", certificateNumber);
        return ResponseEntity.ok(retrievalService.getCertificateMetadata(certificateNumber));
    }

    @GetMapping("/{certificateNumber}/download")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@PathVariable String certificateNumber) {
        log.debug("Generating download URL for {}", certificateNumber);
        String url = retrievalService.generateDownloadUrl(certificateNumber);
        return ResponseEntity.ok(Map.of("downloadUrl", url));
    }

    @PostMapping("/{certificateNumber}/resend")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<Void> resendCertificate(
            @PathVariable String certificateNumber,
            @RequestParam(required = false) String email) {
        log.info("Resend requested for certificate {}. Target email: {}", certificateNumber, email);
        resendService.resendCertificate(certificateNumber, email);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<Void> requestIssuance(@RequestParam Long policyId, @RequestParam BigDecimal amount) {
        log.info("Manual certificate issuance requested. policyId={}, amount={}", policyId, amount);
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found"));

        Application application = applicationRepository.findById(policy.getApplicationId()).orElse(null);
        String email = null;
        String phoneNumber = null;
        
        if (application != null) {
            java.util.Optional<Customer> customer = customerService.getCustomerByUserId(application.getUserId());
            if (customer.isPresent()) {
                email = customer.get().getEmail();
                phoneNumber = customer.get().getPhoneNumber();
            }
        }
        
        certificateService.processCertificateIssuance(policy, amount, email, phoneNumber);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/policy/{policyId}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<List<Certificate>> getCertificates(@PathVariable Long policyId) {
        log.debug("Fetching certificates for policyId={}", policyId);
        return ResponseEntity.ok(certificateRepository.findByPolicyId(policyId));
    }
}
