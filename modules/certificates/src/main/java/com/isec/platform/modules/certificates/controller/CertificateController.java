package com.isec.platform.modules.certificates.controller;

import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.certificates.domain.Certificate;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
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
