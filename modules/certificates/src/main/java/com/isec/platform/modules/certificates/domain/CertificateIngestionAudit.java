package com.isec.platform.modules.certificates.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificate_ingestion_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateIngestionAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, name = "email_message_id")
    private String emailMessageId;

    private String sender;
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngestionStatus status;

    @Column(columnDefinition = "TEXT", name = "failure_reason")
    private String failureReason;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "certificate_id")
    private Long certificateId;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (retryCount == null) retryCount = 0;
    }
}
