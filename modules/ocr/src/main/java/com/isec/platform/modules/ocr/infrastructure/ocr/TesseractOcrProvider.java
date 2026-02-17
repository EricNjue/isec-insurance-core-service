package com.isec.platform.modules.ocr.infrastructure.ocr;

import com.isec.platform.modules.ocr.config.OcrTesseractProperties;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.awt.image.BufferedImage;

@Component
@Slf4j
public class TesseractOcrProvider implements OcrProvider {

    private final ITesseract tesseract;

    public TesseractOcrProvider(OcrTesseractProperties properties) {
        this.tesseract = new Tesseract();
        if (StringUtils.hasText(properties.getDatapath())) {
            this.tesseract.setDatapath(properties.getDatapath());
        }
        this.tesseract.setLanguage(properties.getLanguage());
        this.tesseract.setTessVariable("user_defined_dpi", "300");
    }

    @Override
    public OcrResult extract(BufferedImage image) {
        try {
            String fullText = tesseract.doOCR(image);
            // In production we would use getWords/getLines to get confidence per line
            // tess4j's basic doOCR doesn't return confidence easily without ResultIterator
            return OcrResult.builder()
                    .fullText(fullText)
                    .confidence(java.math.BigDecimal.valueOf(0.80)) // Placeholder default
                    .build();
        } catch (TesseractException e) {
            log.error("Tesseract OCR failed: {}", e.getMessage());
            return OcrResult.builder().fullText("").confidence(java.math.BigDecimal.ZERO).build();
        }
    }
}
