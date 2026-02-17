package com.isec.platform.modules.ocr.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationDto {
    private boolean isValid;
    private List<String> errors;
}
