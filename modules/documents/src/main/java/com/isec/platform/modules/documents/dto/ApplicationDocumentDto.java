package com.isec.platform.modules.documents.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDocumentDto {
    private String documentType;
    private String presignedUrl;
    private String s3Key;
}
