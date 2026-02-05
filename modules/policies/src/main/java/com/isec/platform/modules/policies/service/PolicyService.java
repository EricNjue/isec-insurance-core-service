package com.isec.platform.modules.policies.service;

import com.isec.platform.modules.policies.domain.Policy;
import com.isec.platform.modules.policies.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final PolicyRepository policyRepository;

    @Transactional
    public Policy createPolicy(Long applicationId, BigDecimal totalAnnualPremium) {
        log.info("Creating policy for application: {} with premium: {}", applicationId, totalAnnualPremium);
        
        return policyRepository.findByApplicationId(applicationId)
                .orElseGet(() -> {
                    String policyNumber = "POL-" + System.currentTimeMillis(); // Simple generator for MVP
                    Policy policy = Policy.builder()
                            .applicationId(applicationId)
                            .policyNumber(policyNumber)
                            .startDate(LocalDate.now())
                            .expiryDate(LocalDate.now().plusYears(1).minusDays(1))
                            .totalAnnualPremium(totalAnnualPremium)
                            .balance(totalAnnualPremium)
                            .isActive(true)
                            .build();
                    return policyRepository.save(policy);
                });
    }

    @Transactional(readOnly = true)
    public Optional<Policy> getPolicyByApplicationId(Long applicationId) {
        return policyRepository.findByApplicationId(applicationId);
    }

    @Transactional
    public Policy updateBalance(Long applicationId, BigDecimal amountPaid) {
        Policy policy = policyRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found for application: " + applicationId));
        
        BigDecimal newBalance = policy.getBalance().subtract(amountPaid);
        policy.setBalance(newBalance);
        log.info("Updated balance for policy {}: new balance {}", policy.getPolicyNumber(), newBalance);
        return policyRepository.save(policy);
    }
}
