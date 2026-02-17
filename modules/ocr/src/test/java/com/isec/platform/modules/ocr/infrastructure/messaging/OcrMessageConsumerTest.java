package com.isec.platform.modules.ocr.infrastructure.messaging;

import com.isec.platform.modules.ocr.config.OcrProcessingProperties;
import com.isec.platform.modules.ocr.domain.*;
import com.isec.platform.modules.ocr.infrastructure.ocr.OcrProvider;
import com.isec.platform.modules.ocr.infrastructure.ocr.OcrResult;
import com.isec.platform.modules.ocr.infrastructure.preprocess.ImagePreprocessor;
import com.isec.platform.modules.ocr.infrastructure.text.TextNormalizer;
import com.isec.platform.modules.ocr.repository.OcrAuditLogRepository;
import com.isec.platform.modules.ocr.repository.OcrDocumentRepository;
import com.isec.platform.modules.ocr.repository.OcrFieldRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrMessageConsumerTest {

    @Mock
    private OcrDocumentRepository documentRepository;
    @Mock
    private OcrFieldRepository fieldRepository;
    @Mock
    private OcrAuditLogRepository auditLogRepository;
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
                documentRepository, fieldRepository, auditLogRepository,
                s3Client, preprocessor, ocrProvider, normalizer,
                new OcrProcessingProperties(), meterRegistry
        );
    }

    @Test
    void onOcrDocumentSubmittedProcessesSuccessfully() throws Exception {
        UUID docId = UUID.randomUUID();
        OcrDocument doc = OcrDocument.builder().id(docId).status(OcrStatus.RECEIVED).build();

        Map<String, Object> message = new HashMap<>();
        message.put("documentId", docId.toString());
        message.put("s3Url", "s3://bucket/key.png");
        message.put("documentType", "NTSA_LOGBOOK");

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        BufferedImage testImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(testImage, "png", out);
        ResponseInputStream<?> ris = new ResponseInputStream<>(GetObjectResponse.builder().build(),
                new ByteArrayInputStream(out.toByteArray()));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn((ResponseInputStream) ris);
        when(preprocessor.preprocess(any())).thenReturn(testImage);
        when(ocrProvider.extract(any())).thenReturn(OcrResult.builder().fullText("REG NO: KDA123A").confidence(BigDecimal.ONE).build());
        when(normalizer.normalize(anyString())).thenReturn("REG NO KDA123A");
        when(normalizer.toLines(anyString())).thenReturn(List.of("REG NO KDA123A"));

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
        message.put("s3Url", "s3://bucket/key.png");
        message.put("documentType", "NTSA_LOGBOOK");

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(new RuntimeException("S3 Error"));

        try {
            consumer.onOcrDocumentSubmitted(message);
        } catch (Exception ignore) {}

        verify(documentRepository, atLeastOnce()).save(argThat(d -> d.getStatus() == OcrStatus.FAILED));
    }
}
