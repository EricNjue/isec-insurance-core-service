package com.isec.platform.modules.documents.service;

import com.isec.platform.modules.documents.domain.ValuationLetter;
import com.isec.platform.modules.documents.repository.AuthorizedValuerRepository;
import com.isec.platform.modules.documents.repository.ValuationLetterRepository;
import com.isec.platform.modules.policies.domain.Policy;
import com.isec.platform.modules.policies.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ValuationLetterServiceTest {

    @Mock
    private ValuationLetterRepository letterRepository;
    @Mock
    private AuthorizedValuerRepository valuerRepository;
    @Mock
    private PolicyRepository policyRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private S3Service s3Service;
    @Mock
    private PdfGenerationService pdfGenerationService;
    @Mock
    private PdfSecurityService pdfSecurityService;

    @InjectMocks
    private ValuationLetterService valuationLetterService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(valuationLetterService, "bucketName", "test-bucket");
    }

    @Test
    void testGenerateIfNotExists_ShouldReturnExisting_WhenNotForced() {
        Long policyId = 1L;
        ValuationLetter existing = new ValuationLetter();
        existing.setId(50L);
        when(letterRepository.findFirstByPolicyIdOrderByGeneratedAtDesc(eq(policyId))).thenReturn(Mono.just(existing));

        Mono<ValuationLetter> result = valuationLetterService.generateIfNotExists(policyId, "John Doe", "KAA 001Z", false);

        StepVerifier.create(result)
                .expectNext(existing)
                .verifyComplete();

        verify(letterRepository, never()).save(any());
    }

    @Test
    void testGenerateIfNotExists_ShouldCreateNew_WhenNotFound() {
        Long policyId = 1L;
        Policy policy = Policy.builder().id(policyId).policyNumber("POL-123").build();
        when(letterRepository.findFirstByPolicyIdOrderByGeneratedAtDesc(eq(policyId))).thenReturn(Mono.empty());
        when(policyRepository.findById(policyId)).thenReturn(Mono.just(policy));
        when(valuerRepository.findByActiveTrue()).thenReturn(Flux.empty());
        when(pdfGenerationService.generateValuationLetter(anyMap(), anyList(), any())).thenReturn(new byte[10]);
        when(pdfSecurityService.calculateHash(any())).thenReturn("testhash");
        
        ValuationLetter letter = ValuationLetter.builder()
                .id(100L)
                .policyId(policyId)
                .policyNumber("POL-123")
                .status(ValuationLetter.ValuationLetterStatus.ACTIVE)
                .documentUuid(java.util.UUID.randomUUID())
                .build();
        
        when(letterRepository.save(any(ValuationLetter.class))).thenReturn(Mono.just(letter));
        when(policyRepository.save(any(Policy.class))).thenReturn(Mono.just(policy));

        Mono<ValuationLetter> result = valuationLetterService.generateIfNotExists(policyId, "John Doe", "KAA 001Z", false);

        StepVerifier.create(result)
                .assertNext(res -> {
                    assertNotNull(res);
                    assertEquals(100L, res.getId());
                })
                .verifyComplete();

        verify(s3Service).uploadBytes(eq("test-bucket"), anyString(), any(), eq("application/pdf"));
        verify(letterRepository, times(2)).save(any(ValuationLetter.class));
    }
}
