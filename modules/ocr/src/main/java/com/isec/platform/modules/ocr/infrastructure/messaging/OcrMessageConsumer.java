package com.isec.platform.modules.ocr.infrastructure.messaging;

import com.isec.platform.messaging.RabbitMQConfig;
import com.isec.platform.modules.ocr.domain.*;
import com.isec.platform.modules.ocr.infrastructure.extract.DocumentExtractor;
import com.isec.platform.modules.ocr.infrastructure.extract.NtsaLogbookExtractor;
import com.isec.platform.modules.ocr.infrastructure.extract.NtsaSearchRecordExtractor;
import com.isec.platform.modules.ocr.infrastructure.ocr.OcrProvider;
import com.isec.platform.modules.ocr.infrastructure.text.TextNormalizer;
import com.isec.platform.modules.ocr.infrastructure.preprocess.ImagePreprocessor;
import com.isec.platform.modules.ocr.repository.OcrDocumentRepository;
import com.isec.platform.modules.ocr.repository.OcrFieldRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OcrMessageConsumer {

    private final OcrDocumentRepository documentRepository;
    private final OcrFieldRepository fieldRepository;
    private final S3Client s3Client;
    private final ImagePreprocessor preprocessor;
    private final OcrProvider ocrProvider;
    private final TextNormalizer normalizer;
    private final MeterRegistry meterRegistry;

    @RabbitListener(queues = RabbitMQConfig.OCR_DOCUMENT_SUBMITTED_QUEUE)
    public void onOcrDocumentSubmitted(@Payload Map<String, Object> message) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.info("Received OCR document for processing: {}", message);
            UUID documentId = UUID.fromString(String.valueOf(message.get("documentId")));
            Optional<OcrDocument> optional = documentRepository.findById(documentId);
            if (optional.isEmpty()) {
                log.warn("OCR document not found for id {}", documentId);
                return;
            }
            OcrDocument doc = optional.get();
            doc.setStatus(OcrStatus.PROCESSING);
            documentRepository.save(doc);

            String s3Url = (String) message.get("s3Url");
            String documentType = String.valueOf(message.get("documentType"));

            BufferedImage image = fetchImage(s3Url);
            BufferedImage processed = preprocessor.preprocess(image);
            var ocr = ocrProvider.extract(processed);
            String normalized = normalizer.normalize(ocr.getFullText());

            DocumentExtractor extractor = selectExtractor(documentType);
            Map<String, com.isec.platform.modules.ocr.dto.OcrFieldDto> fields = extractor.extract(normalized);

            // Persist fields
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            for (var entry : fields.entrySet()) {
                OcrField f = OcrField.builder()
                        .id(UUID.randomUUID())
                        .document(doc)
                        .fieldName(entry.getKey())
                        .fieldValue(entry.getValue().getValue())
                        .confidence(entry.getValue().getConfidence())
                        .status(mapStatus(entry.getValue().getStatus()))
                        .build();
                fieldRepository.save(f);
                if (entry.getValue().getConfidence() != null) {
                    sum = sum.add(entry.getValue().getConfidence());
                    count++;
                }
            }
            if (count > 0) {
                doc.setOverallConfidence(sum.divide(BigDecimal.valueOf(count), 4, java.math.RoundingMode.HALF_UP));
            }
            doc.setStatus(OcrStatus.COMPLETED);
            documentRepository.save(doc);

            sample.stop(Timer.builder("ocr.processing.time")
                    .tag("type", documentType)
                    .tag("status", "SUCCESS")
                    .register(meterRegistry));

            meterRegistry.counter("ocr.documents.processed", "type", documentType, "status", "SUCCESS").increment();
            if (doc.getOverallConfidence() != null) {
                meterRegistry.summary("ocr.overall.confidence", "type", documentType)
                        .record(doc.getOverallConfidence().doubleValue());
            }

        } catch (Exception e) {
            log.error("Error handling OCR document submitted message: {}", e.getMessage(), e);
            sample.stop(Timer.builder("ocr.processing.time")
                    .tag("type", String.valueOf(message.get("documentType")))
                    .tag("status", "FAILURE")
                    .register(meterRegistry));
            meterRegistry.counter("ocr.documents.processed", "type", String.valueOf(message.get("documentType")), "status", "FAILURE").increment();
            // set FAILED status for the document if possible
            try {
                UUID documentId = UUID.fromString(String.valueOf(message.get("documentId")));
                documentRepository.findById(documentId).ifPresent(d -> {
                    d.setStatus(OcrStatus.FAILED);
                    documentRepository.save(d);
                });
            } catch (Exception ignore) {}
            // ensure broker handles retry/DLQ
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException(e);
        }
    }

    private DocumentExtractor selectExtractor(String documentType) {
        if ("NTSA_LOGBOOK".equalsIgnoreCase(documentType)) {
            return new NtsaLogbookExtractor();
        }
        return new NtsaSearchRecordExtractor();
    }

    private FieldStatus mapStatus(String s) {
        if (s == null) return null;
        try {
            return FieldStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            return FieldStatus.LOW_CONFIDENCE;
        }
    }

    private BufferedImage fetchImage(String url) throws Exception {
        if (url == null) throw new IllegalArgumentException("s3Url is null");
        if (url.startsWith("s3://")) {
            String without = url.substring(5); // bucket/key
            int slash = without.indexOf('/');
            String bucket = without.substring(0, slash);
            String key = without.substring(slash + 1);
            GetObjectRequest req = GetObjectRequest.builder().bucket(bucket).key(key).build();
            try (ResponseInputStream<?> in = s3Client.getObject(req)) {
                BufferedImage img = ImageIO.read(in);
                if (img == null) {
                    // In tests or for empty files, ImageIO.read might return null
                    return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
                }
                return img;
            }
        }
        try (InputStream in = new URL(url).openStream()) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            return img;
        }
    }
}
