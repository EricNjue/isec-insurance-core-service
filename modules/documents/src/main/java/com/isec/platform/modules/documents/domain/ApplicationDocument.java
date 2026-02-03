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

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private String documentType; // LOGBOOK, NATIONAL_ID, KRA_PIN, VALUATION_REPORT

    @Column(nullable = false)
    private String s3Key;

    @Column(length = 2048)
    private String lastPresignedUrl;

    private LocalDateTime urlExpiryAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
