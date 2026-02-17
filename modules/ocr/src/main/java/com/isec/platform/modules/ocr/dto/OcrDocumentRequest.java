package com.isec.platform.modules.ocr.dto;

import com.isec.platform.modules.ocr.domain.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrDocumentRequest {
    @NotNull
    private UUID tenantId;
    @NotNull
    private DocumentType documentType;
}
