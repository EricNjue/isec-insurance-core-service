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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Mono<Certificate> getCertificateMetadata(@PathVariable String certificateNumber) {
        log.debug("Fetching certificate metadata for {}", certificateNumber);
        return retrievalService.getCertificateMetadata(certificateNumber);
    }

    @GetMapping("/{certificateNumber}/download")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public Mono<Map<String, String>> getDownloadUrl(@PathVariable String certificateNumber) {
        log.debug("Generating download URL for {}", certificateNumber);
        return retrievalService.generateDownloadUrl(certificateNumber)
                .map(url -> Map.of("downloadUrl", url));
    }

    @PostMapping("/{certificateNumber}/resend")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public Mono<Void> resendCertificate(
            @PathVariable String certificateNumber,
            @RequestParam(required = false) String email) {
        log.info("Resend requested for certificate {}. Target email: {}", certificateNumber, email);
        return resendService.resendCertificate(certificateNumber, email);
    }

    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public Mono<Void> requestIssuance(@RequestParam Long policyId, @RequestParam BigDecimal amount) {
        log.info("Manual certificate issuance requested. policyId={}, amount={}", policyId, amount);
        return policyRepository.findById(policyId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Policy not found")))
                .flatMap(policy -> applicationRepository.findById(policy.getApplicationId())
                        .flatMap(application -> customerService.getCustomerByUserId(application.getUserId())
                                .map(customer -> {
                                    return new RecipientInfo(customer.getEmail(), customer.getPhoneNumber());
                                })
                                .defaultIfEmpty(new RecipientInfo(null, null))
                                .flatMap(recipient -> certificateService.processCertificateIssuance(policy, amount, recipient.email, recipient.phone)))
                );
    }

    private record RecipientInfo(String email, String phone) {}

    @GetMapping("/policy/{policyId}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public Flux<Certificate> getCertificates(@PathVariable Long policyId) {
        log.debug("Fetching certificates for policyId={}", policyId);
        return certificateRepository.findByPolicyId(policyId);
    }
}
