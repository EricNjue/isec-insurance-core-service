package com.isec.platform.modules.ocr.infrastructure.messaging;

import com.isec.platform.modules.ocr.domain.*;
import com.isec.platform.modules.ocr.infrastructure.extract.NtsaLogbookExtractor;
import com.isec.platform.modules.ocr.infrastructure.ocr.OcrProvider;
import com.isec.platform.modules.ocr.infrastructure.ocr.OcrResult;
import com.isec.platform.modules.ocr.infrastructure.preprocess.ImagePreprocessor;
import com.isec.platform.modules.ocr.infrastructure.text.TextNormalizer;
import com.isec.platform.modules.ocr.repository.OcrDocumentRepository;
import com.isec.platform.modules.ocr.repository.OcrFieldRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrMessageConsumerTest {

    @Mock
    private OcrDocumentRepository documentRepository;
    @Mock
    private OcrFieldRepository fieldRepository;
    @Mock
    private S3Client s3Client;
    @Mock
    private ImagePreprocessor preprocessor;
    @Mock
    private OcrProvider ocrProvider;
    @Mock
    private TextNormalizer normalizer;

    private MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private OcrMessageConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OcrMessageConsumer(
                documentRepository, fieldRepository, s3Client, preprocessor, ocrProvider, normalizer, meterRegistry
        );
    }

    @Test
    void onOcrDocumentSubmittedProcessesSuccessfully() throws Exception {
        UUID docId = UUID.randomUUID();
        OcrDocument doc = OcrDocument.builder().id(docId).status(OcrStatus.RECEIVED).build();
        
        Map<String, Object> message = new HashMap<>();
        message.put("documentId", docId.toString());
        message.put("s3Url", "s3://bucket/key");
        message.put("documentType", "NTSA_LOGBOOK");

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        
        // Return a real stream but one that ImageIO will handle correctly even if empty/invalid
        // or mock fetchImage behavior by providing a real BufferedImage and avoiding s3Client call if possible.
        // But fetchImage is private. So we mock s3Client to return a valid-looking stream.
        byte[] emptyImage = new byte[0]; 
        InputStream is = new java.io.ByteArrayInputStream(emptyImage);
        ResponseInputStream<?> ris = new ResponseInputStream<>(mock(software.amazon.awssdk.services.s3.model.GetObjectResponse.class), is);
        
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn((ResponseInputStream) ris);
        
        BufferedImage mockImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        
        when(preprocessor.preprocess(any())).thenReturn(mockImage);
        when(ocrProvider.extract(any())).thenReturn(OcrResult.builder().fullText("REG NO: KDA123A").confidence(BigDecimal.ONE).build());
        when(normalizer.normalize(anyString())).thenReturn("REG NO KDA123A");

        consumer.onOcrDocumentSubmitted(message);

        verify(documentRepository, atLeastOnce()).save(any(OcrDocument.class));
        verify(fieldRepository, atLeastOnce()).save(any(OcrField.class));
        verify(documentRepository, atLeastOnce()).save(argThat(d -> d.getStatus() == OcrStatus.COMPLETED));
    }

    @Test
    void onOcrDocumentSubmittedSetsFailedOnException() {
        UUID docId = UUID.randomUUID();
        OcrDocument doc = OcrDocument.builder().id(docId).status(OcrStatus.RECEIVED).build();
        
        Map<String, Object> message = new HashMap<>();
        message.put("documentId", docId.toString());
        message.put("s3Url", "s3://bucket/key");
        message.put("documentType", "NTSA_LOGBOOK");

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(new RuntimeException("S3 Error"));

        try {
            consumer.onOcrDocumentSubmitted(message);
        } catch (Exception ignore) {}

        verify(documentRepository, atLeastOnce()).save(argThat(d -> d.getStatus() == OcrStatus.FAILED));
    }
}
