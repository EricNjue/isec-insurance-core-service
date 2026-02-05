package com.isec.platform.modules.documents.service;

import com.isec.platform.modules.documents.domain.ValuationLetter;
import com.isec.platform.modules.documents.repository.ValuationLetterRepository;
import com.isec.platform.modules.documents.service.DocumentVerificationService.VerificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentVerificationServiceTest {

    @Mock
    private ValuationLetterRepository letterRepository;

    @Mock
    private PdfSecurityService pdfSecurityService;

    @InjectMocks
    private DocumentVerificationService verificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(verificationService, "validityDays", 14);
    }

    @Test
    void testVerifyByUuid_Valid() {
        UUID uuid = UUID.randomUUID();
        ValuationLetter letter = ValuationLetter.builder()
                .documentUuid(uuid)
                .status(ValuationLetter.ValuationLetterStatus.ACTIVE)
                .generatedAt(LocalDateTime.now())
                .documentType("VALUATION_LETTER")
                .build();

        when(letterRepository.findByDocumentUuid(uuid)).thenReturn(Optional.of(letter));

        VerificationResult result = verificationService.verifyByUuid(uuid);

        assertEquals("VALID", result.getStatus());
        assertEquals(uuid.toString(), result.getDocumentId());
        verify(letterRepository, never()).save(any());
    }

    @Test
    void testVerifyByUuid_Expired() {
        UUID uuid = UUID.randomUUID();
        ValuationLetter letter = ValuationLetter.builder()
                .documentUuid(uuid)
                .status(ValuationLetter.ValuationLetterStatus.ACTIVE)
                .generatedAt(LocalDateTime.now().minusDays(15))
                .documentType("VALUATION_LETTER")
                .build();

        when(letterRepository.findByDocumentUuid(uuid)).thenReturn(Optional.of(letter));

        VerificationResult result = verificationService.verifyByUuid(uuid);

        assertEquals("EXPIRED", result.getStatus());
        verify(letterRepository, times(1)).save(letter);
        assertEquals(ValuationLetter.ValuationLetterStatus.EXPIRED, letter.getStatus());
    }

    @Test
    void testVerifyByUuid_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(letterRepository.findByDocumentUuid(uuid)).thenReturn(Optional.empty());

        VerificationResult result = verificationService.verifyByUuid(uuid);

        assertEquals("NOT_FOUND", result.getStatus());
    }

    @Test
    void testVerifyByPdfContent_Valid() {
        UUID uuid = UUID.randomUUID();
        byte[] content = "dummy".getBytes();
        String hash = "hash";
        ValuationLetter letter = ValuationLetter.builder()
                .documentUuid(uuid)
                .status(ValuationLetter.ValuationLetterStatus.ACTIVE)
                .documentHash(hash)
                .generatedAt(LocalDateTime.now())
                .documentType("VALUATION_LETTER")
                .build();

        when(pdfSecurityService.calculateHash(content)).thenReturn(hash);
        when(pdfSecurityService.extractMetadata(content)).thenReturn(Map.of("documentId", uuid.toString()));
        when(letterRepository.findByDocumentUuid(uuid)).thenReturn(Optional.of(letter));

        VerificationResult result = verificationService.verifyByPdfContent(content, "test.pdf");

        assertEquals("VALID", result.getStatus());
        assertEquals("Cryptographic hash matches record", result.getMessage());
    }

    @Test
    void testVerifyByPdfContent_Modified() {
        UUID uuid = UUID.randomUUID();
        byte[] content = "dummy".getBytes();
        ValuationLetter letter = ValuationLetter.builder()
                .documentUuid(uuid)
                .documentHash("original-hash")
                .build();

        when(pdfSecurityService.calculateHash(content)).thenReturn("different-hash");
        when(pdfSecurityService.extractMetadata(content)).thenReturn(Map.of("documentId", uuid.toString()));
        when(letterRepository.findByDocumentUuid(uuid)).thenReturn(Optional.of(letter));

        VerificationResult result = verificationService.verifyByPdfContent(content, "test.pdf");

        assertEquals("MODIFIED", result.getStatus());
        assertEquals("PDF content does not match the original hash", result.getMessage());
    }
}
