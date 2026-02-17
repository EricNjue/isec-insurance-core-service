package com.isec.platform.modules.ocr.infrastructure.messaging;

import com.isec.platform.messaging.RabbitMQConfig;
import com.isec.platform.modules.ocr.config.OcrProcessingProperties;
import com.isec.platform.modules.ocr.domain.*;
import com.isec.platform.modules.ocr.domain.extraction.ExtractionContext;
import com.isec.platform.modules.ocr.infrastructure.extract.DocumentExtractor;
import com.isec.platform.modules.ocr.infrastructure.extract.NtsaLogbookExtractor;
import com.isec.platform.modules.ocr.infrastructure.extract.NtsaSearchRecordExtractor;
import com.isec.platform.modules.ocr.infrastructure.ocr.OcrProvider;
import com.isec.platform.modules.ocr.infrastructure.ocr.OcrResult;
import com.isec.platform.modules.ocr.infrastructure.text.TextNormalizer;
import com.isec.platform.modules.ocr.infrastructure.preprocess.ImagePreprocessor;
import com.isec.platform.modules.ocr.repository.OcrAuditLogRepository;
import com.isec.platform.modules.ocr.repository.OcrDocumentRepository;
import com.isec.platform.modules.ocr.repository.OcrFieldRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OcrMessageConsumer {

    private final OcrDocumentRepository documentRepository;
    private final OcrFieldRepository fieldRepository;
    private final OcrAuditLogRepository auditLogRepository;
    private final S3Client s3Client;
    private final ImagePreprocessor preprocessor;
    private final OcrProvider ocrProvider;
    private final TextNormalizer normalizer;
    private final OcrProcessingProperties processingProperties;
    private final MeterRegistry meterRegistry;

    @RabbitListener(queues = RabbitMQConfig.OCR_DOCUMENT_SUBMITTED_QUEUE)
    public void onOcrDocumentSubmitted(@Payload Map<String, Object> message) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.info("Received OCR document for processing: {}", message);
            UUID documentId = UUID.fromString(String.valueOf(message.get("documentId")));
            String documentType = String.valueOf(message.get("documentType"));
            Optional<OcrDocument> optional = documentRepository.findById(documentId);
            if (optional.isEmpty()) {
                log.warn("OCR document not found for id {}", documentId);
                return;
            }
            OcrDocument doc = optional.get();
            log.info("[OCR] Starting processing for document: {} (Type: {})", documentId, documentType);
            doc.setStatus(OcrStatus.PROCESSING);
            documentRepository.save(doc);

            String s3Url = (String) message.get("s3Url");
            log.debug("[OCR] Fetching document from: {}", s3Url);

            List<BufferedImage> pages = fetchDocumentImages(s3Url);
            if (pages.isEmpty()) {
                throw new IllegalStateException("No pages rendered for OCR");
            }

            StringBuilder rawTextBuilder = new StringBuilder();
            for (int i = 0; i < pages.size(); i++) {
                BufferedImage page = pages.get(i);
                log.debug("[OCR] Preprocessing page {}...", i + 1);
                BufferedImage processed = preprocessor.preprocess(page);

                log.debug("[OCR] Extracting text via Tesseract for page {}...", i + 1);
                OcrResult ocr = ocrProvider.extract(processed);
                if (ocr.getFullText() != null && !ocr.getFullText().isBlank()) {
                    rawTextBuilder.append(ocr.getFullText()).append("\n");
                }
            }

            String rawText = rawTextBuilder.toString();
            String normalized = normalizer.normalize(rawText);
            List<String> lines = normalizer.toLines(normalized);

            log.debug("[OCR] Running field extraction rules...");
            DocumentExtractor extractor = selectExtractor(documentType);
            Map<String, com.isec.platform.modules.ocr.dto.OcrFieldDto> fields = extractor.extract(
                    ExtractionContext.builder()
                            .rawText(rawText)
                            .normalizedText(normalized)
                            .lines(lines)
                            .build());

            log.debug("[OCR] Persisting {} extracted fields", fields.size());
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            int missing = 0;
            for (var entry : fields.entrySet()) {
                com.isec.platform.modules.ocr.dto.OcrFieldDto dto = entry.getValue();
                OcrField f = OcrField.builder()
                        .id(UUID.randomUUID())
                        .document(doc)
                        .fieldName(entry.getKey())
                        .fieldValue(dto.getValue())
                        .confidence(dto.getConfidence())
                        .status(mapStatus(dto.getStatus()))
                        .build();
                fieldRepository.save(f);
                if (dto.getConfidence() != null) {
                    sum = sum.add(dto.getConfidence());
                    count++;
                }
                if (dto.getValue() == null || dto.getValue().isBlank()) {
                    missing++;
                }
            }
            if (count > 0) {
                doc.setOverallConfidence(sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP));
            }
            doc.setStatus(OcrStatus.COMPLETED);
            documentRepository.save(doc);

            log.info("[OCR] Extraction completed for document: {}. Overall Confidence: {}. Fields Extracted: {}",
                    documentId, doc.getOverallConfidence(), count);

            maybeStoreDiagnostics(doc, rawText, normalized, missing);

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
            try {
                UUID documentId = UUID.fromString(String.valueOf(message.get("documentId")));
                documentRepository.findById(documentId).ifPresent(d -> {
                    d.setStatus(OcrStatus.FAILED);
                    documentRepository.save(d);
                });
            } catch (Exception ignore) {}
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

    private void maybeStoreDiagnostics(OcrDocument doc, String rawText, String normalized, int missingFields) {
        if (!processingProperties.isStoreDiagnostics()) return;
        BigDecimal conf = doc.getOverallConfidence();
        double threshold = processingProperties.getDiagnosticsMinConfidence();
        boolean lowConfidence = conf == null || conf.doubleValue() < threshold;
        boolean hasMissing = missingFields > 0;
        if (!lowConfidence && !hasMissing) return;

        String raw = truncate(rawText, processingProperties.getDiagnosticsMaxChars());
        String norm = truncate(normalized, processingProperties.getDiagnosticsMaxChars());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("overallConfidence", conf != null ? conf.doubleValue() : null);
        meta.put("missingFields", missingFields);
        meta.put("rawText", raw);
        meta.put("normalizedText", norm);

        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            String metadata = om.writeValueAsString(meta);
            OcrAuditLog logRec = OcrAuditLog.builder()
                    .id(UUID.randomUUID())
                    .document(doc)
                    .event("OCR_DIAGNOSTICS")
                    .metadata(metadata)
                    .build();
            auditLogRepository.save(logRec);
        } catch (Exception e) {
            log.warn("Failed to store OCR diagnostics: {}", e.getMessage());
        }
    }

    
    private List<BufferedImage> fetchDocumentImages(String url) throws Exception {
        if (url == null) throw new IllegalArgumentException("s3Url is null");
        if (url.startsWith("s3://")) {
            String without = url.substring(5);
            int slash = without.indexOf('/');
            String bucket = without.substring(0, slash);
            String key = without.substring(slash + 1);
            GetObjectRequest req = GetObjectRequest.builder().bucket(bucket).key(key).build();
            try (ResponseInputStream<?> in = s3Client.getObject(req)) {
                byte[] bytes = toByteArray(in);
                return decodeDocument(bytes, key);
            }
        }

        try (InputStream in = new URL(url).openStream()) {
            byte[] bytes = toByteArray(in);
            return decodeDocument(bytes, url);
        }
    }

    private List<BufferedImage> decodeDocument(byte[] bytes, String nameHint) throws Exception {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Empty document content");
        }
        if (isPdf(bytes, nameHint)) {
            return renderPdf(bytes);
        }
        BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        if (img == null) {
            // If decoding failed but looks like a PDF, attempt render
            if (isPdf(bytes, nameHint)) {
                return renderPdf(bytes);
            }
            throw new IllegalStateException("Unable to decode image content");
        }
        return List.of(img);
    }

    private List<BufferedImage> renderPdf(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int pages = Math.min(doc.getNumberOfPages(), processingProperties.getMaxPdfPages());
            List<BufferedImage> images = new ArrayList<>(pages);
            for (int i = 0; i < pages; i++) {
                images.add(renderer.renderImageWithDPI(i, processingProperties.getPdfRenderDpi(), ImageType.RGB));
            }
            return images;
        }
    }

    private boolean isPdf(byte[] bytes, String nameHint) {
        if (nameHint != null && nameHint.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return true;
        }
        return bytes.length >= 4 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
    }

    private byte[] toByteArray(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}
