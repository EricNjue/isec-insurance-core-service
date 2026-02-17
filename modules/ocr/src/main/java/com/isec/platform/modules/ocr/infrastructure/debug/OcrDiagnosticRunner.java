package com.isec.platform.modules.ocr.infrastructure.debug;

import com.isec.platform.modules.ocr.domain.extraction.ExtractionContext;
import com.isec.platform.modules.ocr.infrastructure.extract.DocumentExtractor;
import com.isec.platform.modules.ocr.infrastructure.extract.NtsaLogbookExtractor;
import com.isec.platform.modules.ocr.infrastructure.extract.NtsaSearchRecordExtractor;
import com.isec.platform.modules.ocr.infrastructure.ocr.OcrProvider;
import com.isec.platform.modules.ocr.infrastructure.preprocess.ImagePreprocessor;
import com.isec.platform.modules.ocr.infrastructure.text.TextNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Diagnostic runner to execute OCR pipeline on a single file for debugging.
 * Enable with: -Docr.debug.enabled=true -Docr.debug.file=/abs/path.pdf -Docr.debug.documentType=NTSA_SEARCH_RECORD
 */
@Component
@ConditionalOnProperty(prefix = "ocr.debug", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class OcrDiagnosticRunner implements ApplicationRunner {

    private final ImagePreprocessor preprocessor;
    private final OcrProvider ocrProvider;
    private final TextNormalizer normalizer;

    @Value("${ocr.debug.file:}")
    private String debugFile;

    @Value("${ocr.debug.documentType:NTSA_SEARCH_RECORD}")
    private String documentType;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (debugFile == null || debugFile.isBlank()) {
            log.warn("[OCR-DEBUG] ocr.debug.enabled=true but no ocr.debug.file provided");
            return;
        }
        File f = new File(debugFile);
        if (!f.exists()) {
            log.error("[OCR-DEBUG] File not found: {}", f.getAbsolutePath());
            return;
        }
        log.info("[OCR-DEBUG] Running diagnostic OCR for file: {} (type: {})", f.getAbsolutePath(), documentType);

        BufferedImage image = loadImage(f);
        if (image == null) {
            log.error("[OCR-DEBUG] Failed to load image from file: {}", f.getAbsolutePath());
            return;
        }

        BufferedImage processed = preprocessor.preprocess(image);
        var ocr = ocrProvider.extract(processed);
        String fullText = ocr.getFullText();
        String normalized = normalizer.normalize(fullText);
        List<String> lines = normalizer.toLines(normalized);
        log.info("[OCR-DEBUG] OCR raw text (first 1000 chars):\n{}", preview(fullText, 1000));
        log.info("[OCR-DEBUG] OCR normalized text (first 1000 chars):\n{}", preview(normalized, 1000));

        DocumentExtractor extractor = selectExtractor(documentType);
        Map<String, com.isec.platform.modules.ocr.dto.OcrFieldDto> fields = extractor.extract(
                ExtractionContext.builder()
                        .rawText(fullText)
                        .normalizedText(normalized)
                        .lines(lines)
                        .build());
        log.info("[OCR-DEBUG] Extracted {} fields:", fields.size());
        fields.forEach((k, v) -> log.info("  - {} => value='{}', confidence={}, status={}", k, v.getValue(), v.getConfidence(), v.getStatus()));
    }

    private BufferedImage loadImage(File f) {
        try {
            String name = f.getName().toLowerCase();
            if (name.endsWith(".pdf")) {
                try (PDDocument doc = Loader.loadPDF(f)) {
                    PDFRenderer renderer = new PDFRenderer(doc);
                    return renderer.renderImageWithDPI(0, 300, ImageType.RGB);
                }
            }
            return ImageIO.read(f);
        } catch (Exception e) {
            log.error("[OCR-DEBUG] Error loading file {}: {}", f.getAbsolutePath(), e.getMessage(), e);
            return null;
        }
    }

    private DocumentExtractor selectExtractor(String documentType) {
        if ("NTSA_LOGBOOK".equalsIgnoreCase(documentType)) {
            return new NtsaLogbookExtractor();
        }
        return new NtsaSearchRecordExtractor();
    }

    private static String preview(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
