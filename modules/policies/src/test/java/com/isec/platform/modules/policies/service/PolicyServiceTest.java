package com.isec.platform.modules.policies.service;

import com.isec.platform.modules.policies.domain.Policy;
import com.isec.platform.modules.policies.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private PolicyService policyService;

    private final Long applicationId = 1L;
    private final BigDecimal totalPremium = new BigDecimal("5000");

    @Test
    void createPolicy_ExistingPolicy_ReturnsExisting() {
        Policy existingPolicy = Policy.builder()
                .applicationId(applicationId)
                .policyNumber("POL-EXISTING")
                .build();
        
        when(policyRepository.findByApplicationId(applicationId)).thenReturn(Mono.just(existingPolicy));

        Mono<Policy> result = policyService.createPolicy(applicationId, totalPremium);

        StepVerifier.create(result)
                .assertNext(policy -> {
                    assertEquals(existingPolicy.getPolicyNumber(), policy.getPolicyNumber());
                })
                .verifyComplete();
        
        verify(policyRepository, never()).save(any(Policy.class));
    }

    @Test
    void createPolicy_NewPolicy_CreatesAndSaves() {
        when(policyRepository.findByApplicationId(applicationId)).thenReturn(Mono.empty());
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<Policy> result = policyService.createPolicy(applicationId, totalPremium);

        StepVerifier.create(result)
                .assertNext(policy -> {
                    assertNotNull(policy);
                    assertEquals(applicationId, policy.getApplicationId());
                    assertEquals(totalPremium, policy.getTotalAnnualPremium());
                    assertEquals(totalPremium, policy.getBalance());
                    assertTrue(policy.getIsActive());
                    assertNotNull(policy.getPolicyNumber());
                    assertTrue(policy.getPolicyNumber().startsWith("POL-"));
                })
                .verifyComplete();
        
        verify(policyRepository).save(any(Policy.class));
    }

    @Test
    void getPolicyByApplicationId_Found() {
        Policy policy = Policy.builder().applicationId(applicationId).build();
        when(policyRepository.findByApplicationId(applicationId)).thenReturn(Mono.just(policy));

        Mono<Policy> result = policyService.getPolicyByApplicationId(applicationId);

        StepVerifier.create(result)
                .assertNext(p -> {
                    assertEquals(applicationId, p.getApplicationId());
                })
                .verifyComplete();
    }

    @Test
    void getPolicyByApplicationId_NotFound() {
        when(policyRepository.findByApplicationId(applicationId)).thenReturn(Mono.empty());

        Mono<Policy> result = policyService.getPolicyByApplicationId(applicationId);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void updateBalance_Success() {
        Policy policy = Policy.builder()
                .applicationId(applicationId)
                .policyNumber("POL-123")
                .balance(new BigDecimal("5000"))
                .build();
        
        when(policyRepository.findByApplicationId(applicationId)).thenReturn(Mono.just(policy));
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        BigDecimal amountPaid = new BigDecimal("1500");
        Mono<Policy> result = policyService.updateBalance(applicationId, amountPaid);

        StepVerifier.create(result)
                .assertNext(p -> {
                    assertEquals(new BigDecimal("3500"), p.getBalance());
                })
                .verifyComplete();
        
        verify(policyRepository).save(policy);
    }

    @Test
    void updateBalance_NotFound_ThrowsException() {
        when(policyRepository.findByApplicationId(applicationId)).thenReturn(Mono.empty());

        BigDecimal amountPaid = new BigDecimal("1500");
        Mono<Policy> result = policyService.updateBalance(applicationId, amountPaid);

        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
        
        verify(policyRepository, never()).save(any(Policy.class));
    }
}
