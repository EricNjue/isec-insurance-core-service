package com.isec.platform.modules.ocr.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OcrExtractionResultDto {
    private UUID documentId;
    private UUID tenantId;
    private String documentType;
    private String s3Url; // internal or presigned URL depending on context
    private Instant extractedAt;
    private BigDecimal overallConfidence;
    private Map<String, OcrFieldDto> fields;
    private ValidationDto validation;
}
