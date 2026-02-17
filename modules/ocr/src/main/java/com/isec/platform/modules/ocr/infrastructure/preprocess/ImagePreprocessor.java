package com.isec.platform.modules.ocr.infrastructure.preprocess;

import java.awt.image.BufferedImage;

public interface ImagePreprocessor {
    BufferedImage preprocess(BufferedImage input);
}
