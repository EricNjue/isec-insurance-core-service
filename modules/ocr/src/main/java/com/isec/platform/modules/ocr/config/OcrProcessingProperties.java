package com.isec.platform.modules.ocr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ocr.processing")
public class OcrProcessingProperties {
    /** PDF render DPI for OCR. */
    private int pdfRenderDpi = 300;

    /** Max number of PDF pages to OCR. */
    private int maxPdfPages = 3;

    /** Store OCR diagnostic text when confidence is low. */
    private boolean storeDiagnostics = true;

    /** Confidence threshold below which diagnostics are stored. */
    private double diagnosticsMinConfidence = 0.75;

    /** Max characters of OCR text to store in diagnostics. */
    private int diagnosticsMaxChars = 8000;
}
