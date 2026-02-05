package com.isec.platform.modules.policies.service;

import com.isec.platform.modules.policies.domain.Policy;
import com.isec.platform.modules.policies.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

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
        
        when(policyRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(existingPolicy));

        Policy result = policyService.createPolicy(applicationId, totalPremium);

        assertEquals(existingPolicy.getPolicyNumber(), result.getPolicyNumber());
        verify(policyRepository, never()).save(any(Policy.class));
    }

    @Test
    void createPolicy_NewPolicy_CreatesAndSaves() {
        when(policyRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Policy result = policyService.createPolicy(applicationId, totalPremium);

        assertNotNull(result);
        assertEquals(applicationId, result.getApplicationId());
        assertEquals(totalPremium, result.getTotalAnnualPremium());
        assertEquals(totalPremium, result.getBalance());
        assertTrue(result.getIsActive());
        assertNotNull(result.getPolicyNumber());
        assertTrue(result.getPolicyNumber().startsWith("POL-"));
        
        verify(policyRepository).save(any(Policy.class));
    }

    @Test
    void getPolicyByApplicationId_Found() {
        Policy policy = Policy.builder().applicationId(applicationId).build();
        when(policyRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(policy));

        Optional<Policy> result = policyService.getPolicyByApplicationId(applicationId);

        assertTrue(result.isPresent());
        assertEquals(applicationId, result.get().getApplicationId());
    }

    @Test
    void getPolicyByApplicationId_NotFound() {
        when(policyRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());

        Optional<Policy> result = policyService.getPolicyByApplicationId(applicationId);

        assertFalse(result.isPresent());
    }

    @Test
    void updateBalance_Success() {
        Policy policy = Policy.builder()
                .applicationId(applicationId)
                .policyNumber("POL-123")
                .balance(new BigDecimal("5000"))
                .build();
        
        when(policyRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal amountPaid = new BigDecimal("1500");
        Policy result = policyService.updateBalance(applicationId, amountPaid);

        assertEquals(new BigDecimal("3500"), result.getBalance());
        verify(policyRepository).save(policy);
    }

    @Test
    void updateBalance_NotFound_ThrowsException() {
        when(policyRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());

        BigDecimal amountPaid = new BigDecimal("1500");
        assertThrows(IllegalArgumentException.class, () -> 
            policyService.updateBalance(applicationId, amountPaid)
        );
        
        verify(policyRepository, never()).save(any(Policy.class));
    }
}
