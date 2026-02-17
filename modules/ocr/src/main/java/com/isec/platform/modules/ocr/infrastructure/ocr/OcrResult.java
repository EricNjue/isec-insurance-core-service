package com.isec.platform.modules.ocr.infrastructure.ocr;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class OcrResult {
    private final String fullText;
    private final List<OcrLine> lines;
    private final BigDecimal confidence;

    @Getter
    @Builder
    public static class OcrLine {
        private final String text;
        private final BigDecimal confidence;
        // Optionally add bounding box info if needed for proximity matching
    }
}
