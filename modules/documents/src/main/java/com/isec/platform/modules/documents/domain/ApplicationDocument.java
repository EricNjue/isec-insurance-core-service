package com.isec.platform.modules.documents.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "document_type", nullable = false)
    private String documentType; // LOGBOOK, NATIONAL_ID, KRA_PIN, VALUATION_REPORT

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "last_presigned_url", length = 2048)
    private String lastPresignedUrl;

    @Column(name = "url_expiry_at")
    private LocalDateTime urlExpiryAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
