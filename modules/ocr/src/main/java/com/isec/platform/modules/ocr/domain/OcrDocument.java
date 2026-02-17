package com.isec.platform.modules.ocr.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ocr_document", indexes = {
        @Index(name = "idx_ocr_document_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_ocr_document_hash", columnList = "document_hash"),
        @Index(name = "idx_ocr_document_type", columnList = "document_type")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrDocument {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 50, nullable = false)
    private DocumentType documentType;

    @Column(name = "s3_url", length = 255)
    private String s3Url;

    @Column(name = "document_hash", length = 64, nullable = false)
    private String documentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OcrStatus status;

    @Column(name = "overall_confidence", precision = 5, scale = 4)
    private BigDecimal overallConfidence;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
