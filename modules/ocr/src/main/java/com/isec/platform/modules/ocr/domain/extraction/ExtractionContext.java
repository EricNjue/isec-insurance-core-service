package com.isec.platform.modules.ocr.domain.extraction;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExtractionContext {
    private final String normalizedText;
    private final List<String> lines;
}
