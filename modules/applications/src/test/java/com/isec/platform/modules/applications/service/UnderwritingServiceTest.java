package com.isec.platform.modules.applications.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.common.exception.ResourceNotFoundException;
import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.domain.ApplicationStatus;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.policies.service.PolicyService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnderwritingServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UnderwritingService underwritingService;

    private Application application;

    @BeforeEach
    void setUp() {
        application = Application.builder()
                .id(1L)
                .status(ApplicationStatus.UNDERWRITING_REVIEW)
                .pricingSnapshot("{\"totalPremium\": 50000.00}")
                .build();
    }

    @Test
    void shouldApproveApplication() throws Exception {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        Application approved = underwritingService.approve(1L, "uw1", "Approved");

        assertEquals(ApplicationStatus.APPROVED_PENDING_PAYMENT, approved.getStatus());
        assertEquals("uw1", approved.getUnderwriterId());
        assertEquals("Approved", approved.getUnderwriter_comments());
        
        verify(policyService).createPolicy(eq(1L), any());
    }

    @Test
    void shouldDeclineApplication() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        Application declined = underwritingService.decline(1L, "uw1", "Declined", "High risk");

        assertEquals(ApplicationStatus.DECLINED, declined.getStatus());
        assertEquals("High risk", declined.getReferralReason());
    }

    @Test
    void shouldThrowExceptionWhenApplicationNotFound() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> underwritingService.approve(1L, "uw1", "Approved"));
    }
}
