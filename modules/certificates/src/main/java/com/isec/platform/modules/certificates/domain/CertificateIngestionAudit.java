package com.isec.platform.modules.certificates.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDateTime;

@Table("certificate_ingestion_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateIngestionAudit {
    @Id
    private Long id;

    private String emailMessageId;

    private String sender;
    private String subject;

    private IngestionStatus status;

    private String failureReason;

    private Integer retryCount;

    private Long certificateId;

    private LocalDateTime processedAt;

    private LocalDateTime createdAt;
}
