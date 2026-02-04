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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
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
        when(letterRepository.findFirstByPolicyIdAndGeneratedAtAfter(eq(policyId), any())).thenReturn(Optional.of(existing));

        ValuationLetter result = valuationLetterService.generateIfNotExists(policyId, "John Doe", "KAA 001Z", false);

        assertEquals(existing, result);
        verify(letterRepository, never()).save(any());
    }

    @Test
    void testGenerateIfNotExists_ShouldCreateNew_WhenNotFound() {
        Long policyId = 1L;
        Policy policy = Policy.builder().id(policyId).policyNumber("POL-123").build();
        when(letterRepository.findFirstByPolicyIdAndGeneratedAtAfter(eq(policyId), any())).thenReturn(Optional.empty());
        when(policyRepository.findById(policyId)).thenReturn(Optional.of(policy));
        when(valuerRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
        when(pdfGenerationService.generateValuationLetter(anyMap(), anyList())).thenReturn(new byte[10]);
        
        ValuationLetter letter = new ValuationLetter();
        letter.setId(100L);
        when(letterRepository.save(any(ValuationLetter.class))).thenReturn(letter);

        ValuationLetter result = valuationLetterService.generateIfNotExists(policyId, "John Doe", "KAA 001Z", false);

        assertNotNull(result);
        verify(s3Service).uploadBytes(eq("test-bucket"), anyString(), any(), eq("application/pdf"));
        verify(letterRepository, times(2)).save(any(ValuationLetter.class));
    }
}
