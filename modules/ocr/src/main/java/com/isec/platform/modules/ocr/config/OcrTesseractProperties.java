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
    /** Language code (e.g., 'eng', 'fra') */
    private String language = "eng";
}
