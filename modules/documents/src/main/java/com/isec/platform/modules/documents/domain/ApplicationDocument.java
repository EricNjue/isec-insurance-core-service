package com.isec.platform.modules.documents.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDateTime;

@Table("application_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDocument {
    @Id
    private Long id;

    private Long applicationId;

    private String documentType; // LOGBOOK, NATIONAL_ID, KRA_PIN, VALUATION_REPORT

    private String s3Key;

    private String lastPresignedUrl;

    private LocalDateTime urlExpiryAt;

    private LocalDateTime createdAt;
}
