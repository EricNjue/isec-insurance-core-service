package com.isec.platform.modules.ocr.infrastructure.extract;

import com.isec.platform.modules.ocr.dto.OcrFieldDto;
import java.util.Map;

public interface DocumentExtractor {
    Map<String, OcrFieldDto> extract(String fullText);
}
