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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Map;

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
        when(applicationRepository.findById(1L)).thenReturn(Mono.just(application));
        when(policyService.createPolicy(eq(1L), any())).thenReturn(Mono.just(new com.isec.platform.modules.policies.domain.Policy()));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> Mono.just(i.getArguments()[0]));
        
        Map<String, Object> pricingMap = Map.of("totalPremium", 50000.00);
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(pricingMap);

        StepVerifier.create(underwritingService.approve(1L, "uw1", "Approved"))
                .assertNext(approved -> {
                    assertEquals(ApplicationStatus.APPROVED_PENDING_PAYMENT, approved.getStatus());
                    assertEquals("uw1", approved.getUnderwriterId());
                    assertEquals("Approved", approved.getUnderwriter_comments());
                })
                .verifyComplete();

        verify(policyService).createPolicy(eq(1L), any());
    }

    @Test
    void shouldDeclineApplication() {
        when(applicationRepository.findById(1L)).thenReturn(Mono.just(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> Mono.just(i.getArguments()[0]));

        StepVerifier.create(underwritingService.decline(1L, "uw1", "Declined", "High risk"))
                .assertNext(declined -> {
                    assertEquals(ApplicationStatus.DECLINED, declined.getStatus());
                    assertEquals("High risk", declined.getReferralReason());
                })
                .verifyComplete();
    }

    @Test
    void shouldThrowExceptionWhenApplicationNotFound() {
        when(applicationRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(underwritingService.approve(1L, "uw1", "Approved"))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }
}
