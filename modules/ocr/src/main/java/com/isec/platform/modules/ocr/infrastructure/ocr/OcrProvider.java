package com.isec.platform.modules.ocr.infrastructure.ocr;

import java.awt.image.BufferedImage;

public interface OcrProvider {
    OcrResult extract(BufferedImage image);
}
