package com.isec.platform.modules.ocr.infrastructure.extract;

import com.isec.platform.modules.ocr.domain.extraction.ExtractionContext;
import com.isec.platform.modules.ocr.dto.OcrFieldDto;

import java.util.Map;

public interface DocumentExtractor {
    Map<String, OcrFieldDto> extract(ExtractionContext context);
}
