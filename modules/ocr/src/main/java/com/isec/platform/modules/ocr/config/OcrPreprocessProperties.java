package com.isec.platform.modules.ocr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ocr.preprocess")
public class OcrPreprocessProperties {
    /** Enable preprocessing with OpenCV. */
    private boolean enabled = true;

    /** Apply deskew correction when possible. */
    private boolean deskewEnabled = true;

    /** Scale factor applied before thresholding. */
    private double scaleFactor = 1.2;

    /** Median blur kernel size (odd number). */
    private int medianBlurKernel = 3;

    /** Use adaptive thresholding instead of Otsu. */
    private boolean adaptiveThreshold = true;

    /** Adaptive threshold block size (odd number). */
    private int adaptiveBlockSize = 15;

    /** Adaptive threshold C value. */
    private double adaptiveC = 3.0;

    /** CLAHE clip limit. */
    private double claheClipLimit = 2.0;

    /** CLAHE tile size. */
    private int claheTileSize = 8;
}
