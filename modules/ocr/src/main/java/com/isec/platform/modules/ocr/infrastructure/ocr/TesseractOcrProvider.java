package com.isec.platform.modules.ocr.infrastructure.ocr;

import com.isec.platform.modules.ocr.config.OcrTesseractProperties;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Primary
public class TesseractOcrProvider implements OcrProvider {

    private ITesseract tesseract;
    private final OcrTesseractProperties properties;

    public TesseractOcrProvider(OcrTesseractProperties properties) {
        this.properties = properties;
        try {
            String libPath = properties.getLibraryPath();
            if (StringUtils.hasText(libPath)) {
                File path = new File(libPath);
                if (path.exists()) {
                    String absolutePath = path.getAbsolutePath();
                    File libFolder = new File(path, "lib");
                    if (libFolder.exists() && libFolder.isDirectory()) {
                        absolutePath = libFolder.getAbsolutePath();
                    }

                    System.setProperty("jna.library.path", absolutePath);
                    com.sun.jna.NativeLibrary.addSearchPath("tesseract", absolutePath);
                    log.info("Setting jna.library.path and Tesseract search path to: {}", absolutePath);
                } else {
                    log.warn("Provided TESSERACT_LIBRARY_PATH does not exist: {}", libPath);
                }
            }

            this.tesseract = new Tesseract();
            if (StringUtils.hasText(properties.getDatapath())) {
                this.tesseract.setDatapath(properties.getDatapath());
            }
            this.tesseract.setLanguage(properties.getLanguage());
            this.tesseract.setPageSegMode(properties.getPageSegMode());
            this.tesseract.setOcrEngineMode(properties.getOcrEngineMode());
            this.tesseract.setTessVariable("user_defined_dpi", String.valueOf(properties.getUserDefinedDpi()));
            if (properties.isPreserveInterwordSpaces()) {
                this.tesseract.setTessVariable("preserve_interword_spaces", "1");
            }
            if (StringUtils.hasText(properties.getCharWhitelist())) {
                this.tesseract.setTessVariable("tessedit_char_whitelist", properties.getCharWhitelist());
            }
            if (StringUtils.hasText(properties.getCharBlacklist())) {
                this.tesseract.setTessVariable("tessedit_char_blacklist", properties.getCharBlacklist());
            }
            log.info("TesseractOcrProvider initialized successfully");
        } catch (Throwable e) {
            log.warn("Tesseract initialization failed: {}. OCR will be disabled.", e.getMessage());
            this.tesseract = null;
        }
    }

    @Override
    public OcrResult extract(BufferedImage image) {
        if (this.tesseract == null) {
            return OcrResult.builder().fullText("").confidence(BigDecimal.ZERO).lines(List.of()).build();
        }
        try {
            String fullText = tesseract.doOCR(image);
            List<OcrResult.OcrLine> lines = new ArrayList<>();
            try {
                List<Word> words = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);
                for (Word w : words) {
                    if (w.getText() == null || w.getText().isBlank()) continue;
                    BigDecimal conf = BigDecimal.valueOf(w.getConfidence() / 100.0).setScale(4, RoundingMode.HALF_UP);
                    lines.add(OcrResult.OcrLine.builder().text(w.getText()).confidence(conf).build());
                }
            } catch (Exception e) {
                log.debug("Failed to get OCR line confidences: {}", e.getMessage());
            }

            BigDecimal avgConf = averageConfidence(lines);
            return OcrResult.builder()
                    .fullText(fullText)
                    .lines(lines)
                    .confidence(avgConf != null ? avgConf : BigDecimal.ZERO)
                    .build();
        } catch (TesseractException e) {
            log.error("Tesseract OCR failed: {}", e.getMessage());
            return OcrResult.builder().fullText("").confidence(BigDecimal.ZERO).lines(List.of()).build();
        }
    }

    private BigDecimal averageConfidence(List<OcrResult.OcrLine> lines) {
        if (lines == null || lines.isEmpty()) return null;
        double avg = lines.stream()
                .map(OcrResult.OcrLine::getConfidence)
                .filter(c -> c != null)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);
        return BigDecimal.valueOf(avg).setScale(4, RoundingMode.HALF_UP);
    }
}
