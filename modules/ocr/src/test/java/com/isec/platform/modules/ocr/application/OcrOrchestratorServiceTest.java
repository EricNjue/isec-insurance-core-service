package com.isec.platform.modules.ocr.application;

import com.isec.platform.modules.ocr.domain.DocumentType;
import com.isec.platform.modules.ocr.domain.OcrDocument;
import com.isec.platform.modules.ocr.domain.OcrStatus;
import com.isec.platform.modules.ocr.dto.OcrExtractionResultDto;
import com.isec.platform.modules.ocr.infrastructure.messaging.OcrMessageProducer;
import com.isec.platform.modules.ocr.repository.OcrAuditLogRepository;
import com.isec.platform.modules.ocr.repository.OcrDocumentRepository;
import com.isec.platform.modules.ocr.repository.OcrFieldRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrOrchestratorServiceTest {

    @Mock
    private OcrDocumentRepository documentRepository;
    @Mock
    private OcrFieldRepository fieldRepository;
    @Mock
    private OcrAuditLogRepository auditLogRepository;
    @Mock
    private OcrMessageProducer messageProducer;

    private OcrOrchestratorService service;

    @BeforeEach
    void setUp() {
        service = new OcrOrchestratorService(documentRepository, fieldRepository, auditLogRepository, messageProducer);
    }

    @Test
    void submitNewDocumentSavesAndPublishes() {
        UUID tenantId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy content".getBytes());
        String s3Url = "s3://bucket/test.pdf";

        when(documentRepository.findByTenantIdAndDocumentHash(any(), any())).thenReturn(Optional.empty());

        UUID resultId = service.submit(tenantId, DocumentType.NTSA_LOGBOOK, file, s3Url);

        assertNotNull(resultId);
        verify(documentRepository, times(1)).save(any(OcrDocument.class));
        verify(messageProducer, times(1)).publishDocumentSubmitted(eq(resultId), eq(tenantId), anyString(), eq(s3Url), anyString());
    }

    @Test
    void submitDuplicateDocumentReturnsExistingId() {
        UUID tenantId = UUID.randomUUID();
        UUID existingId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy content".getBytes());
        
        OcrDocument existing = OcrDocument.builder().id(existingId).build();
        when(documentRepository.findByTenantIdAndDocumentHash(any(), any())).thenReturn(Optional.of(existing));

        UUID resultId = service.submit(tenantId, DocumentType.NTSA_LOGBOOK, file, "s3://url");

        assertEquals(existingId, resultId);
        verify(documentRepository, never()).save(any());
        verify(messageProducer, never()).publishDocumentSubmitted(any(), any(), any(), any(), any());
    }

    @Test
    void getExtractionReturnsDtoWithS3Url() {
        UUID docId = UUID.randomUUID();
        OcrDocument doc = OcrDocument.builder()
                .id(docId)
                .tenantId(UUID.randomUUID())
                .documentType(DocumentType.NTSA_LOGBOOK)
                .status(OcrStatus.COMPLETED)
                .s3Url("s3://bucket/key")
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(fieldRepository.findByDocumentId(docId)).thenReturn(java.util.List.of());

        Optional<OcrExtractionResultDto> result = service.getExtraction(docId);

        assertTrue(result.isPresent());
        assertEquals("s3://bucket/key", result.get().getS3Url());
    }
}
