package com.isec.platform.modules.ocr.infrastructure.ocr;

import lombok.Setter;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;

@Setter
public class MockOcrProvider implements OcrProvider {
    private String mockText = "";
    private BigDecimal mockConfidence = BigDecimal.valueOf(0.95);

    @Override
    public OcrResult extract(BufferedImage image) {
        return OcrResult.builder()
                .fullText(mockText)
                .confidence(mockConfidence)
                .build();
    }
}
