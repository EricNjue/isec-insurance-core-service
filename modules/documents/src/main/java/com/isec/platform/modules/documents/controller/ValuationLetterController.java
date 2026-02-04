package com.isec.platform.modules.documents.controller;

import com.isec.platform.modules.documents.domain.ValuationLetter;
import com.isec.platform.modules.documents.repository.ValuationLetterRepository;
import com.isec.platform.modules.documents.service.ValuationLetterService;
import com.isec.platform.modules.policies.domain.Policy;
import com.isec.platform.modules.policies.repository.PolicyRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ValuationLetterController {

    private final ValuationLetterService valuationLetterService;
    private final ValuationLetterRepository valuationLetterRepository;
    private final PolicyRepository policyRepository;

    @PostMapping("/policies/{policyId}/valuation-letter")
    @PreAuthorize("hasAnyRole('ADMIN','SYSTEM')")
    public ResponseEntity<Map<String, Object>> generate(@PathVariable Long policyId,
                                                        @RequestParam(defaultValue = "false") boolean force,
                                                        @RequestBody GenerateRequest req) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));

        ValuationLetter letter = valuationLetterService.generateIfNotExists(policyId,
                req.getInsuredName(), req.getRegistrationNumber(), force);

        String downloadUrl = valuationLetterService.generateDownloadUrl(letter);

        return ResponseEntity.ok(Map.of(
                "id", letter.getId(),
                "policyId", letter.getPolicyId(),
                "policyNumber", letter.getPolicyNumber(),
                "vehicleRegistrationNumber", letter.getVehicleRegistrationNumber(),
                "status", letter.getStatus().name(),
                "s3Key", letter.getPdfS3Key(),
                "presignedUrl", downloadUrl
        ));
    }

    @GetMapping("/valuation-letters/{id}")
    @PreAuthorize("hasAnyRole('RETAIL_USER','AGENT','ADMIN')")
    public ResponseEntity<ValuationLetter> get(@PathVariable Long id) {
        return valuationLetterRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/valuation-letters/{id}/download")
    @PreAuthorize("hasAnyRole('RETAIL_USER','AGENT','ADMIN')")
    public ResponseEntity<Map<String, String>> download(@PathVariable Long id) {
        ValuationLetter letter = valuationLetterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Valuation letter not found: " + id));
        String url = valuationLetterService.generateDownloadUrl(letter);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/valuation-letters/policy/{policyId}")
    @PreAuthorize("hasAnyRole('RETAIL_USER','AGENT','ADMIN')")
    public ResponseEntity<ValuationLetter> getLatestByPolicy(@PathVariable Long policyId) {
        return valuationLetterRepository.findFirstByPolicyIdOrderByGeneratedAtDesc(policyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/valuation-letters/policy-number/{policyNumber}")
    @PreAuthorize("hasAnyRole('RETAIL_USER','AGENT','ADMIN')")
    public ResponseEntity<ValuationLetter> getLatestByPolicyNumber(@PathVariable String policyNumber) {
        return valuationLetterRepository.findFirstByPolicyNumberOrderByGeneratedAtDesc(policyNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Data
    public static class GenerateRequest {
        private String insuredName;
        private String registrationNumber;
        private String recipientEmail;
    }
}
