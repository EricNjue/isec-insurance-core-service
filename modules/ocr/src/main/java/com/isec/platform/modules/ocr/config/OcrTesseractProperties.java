package com.isec.platform.modules.ocr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ocr.tesseract")
public class OcrTesseractProperties {
    /** Path to tessdata directory. If empty, uses system default (TESSDATA_PREFIX env) */
    private String datapath;
    /** Path to Tesseract native library directory (e.g., /opt/homebrew/lib) */
    private String libraryPath;
    /** Language code (e.g., 'eng', 'fra') */
    private String language = "eng";
    /** Tesseract page segmentation mode (PSM). */
    private int pageSegMode = 6;
    /** Tesseract OCR engine mode (OEM). */
    private int ocrEngineMode = 1;
    /** Preserve interword spaces to improve label/value parsing. */
    private boolean preserveInterwordSpaces = true;
    /** User-defined DPI for consistent OCR. */
    private int userDefinedDpi = 300;
    /** Optional character whitelist for OCR. */
    private String charWhitelist;
    /** Optional character blacklist for OCR. */
    private String charBlacklist;
}
