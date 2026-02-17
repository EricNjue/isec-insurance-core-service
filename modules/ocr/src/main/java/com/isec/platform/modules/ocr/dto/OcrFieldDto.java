package com.isec.platform.modules.ocr.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrFieldDto {
    private String value;
    private BigDecimal confidence;
    private String status;
}
