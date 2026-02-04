package com.isec.platform.modules.policies.controller;

import com.isec.platform.modules.policies.domain.Policy;
import com.isec.platform.modules.policies.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Slf4j
public class PolicyController {

    private final PolicyRepository policyRepository;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<Policy> getPolicy(@PathVariable Long id) {
        log.debug("Fetching policy by id={}", id);
        return policyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<Policy> getPolicyByApplication(@PathVariable Long applicationId) {
        log.debug("Fetching policy by applicationId={}", applicationId);
        return policyRepository.findByApplicationId(applicationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
