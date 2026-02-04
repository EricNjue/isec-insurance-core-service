package com.isec.platform.modules.documents.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "valuation_letters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValuationLetter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "certificate_id")
    private Long certificateId;

    @Column(name = "vehicle_registration_number", nullable = false)
    private String vehicleRegistrationNumber;

    @Column(name = "insured_name", nullable = false)
    private String insuredName;

    @Column(name = "policy_number", nullable = false)
    private String policyNumber;

    @Column(name = "pdf_s3_bucket")
    private String pdfS3Bucket;

    @Column(name = "pdf_s3_key")
    private String pdfS3Key;

    @Column(name = "generated_by")
    private String generatedBy;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValuationLetterStatus status;

    public enum ValuationLetterStatus {
        GENERATED, SENT, REVOKED
    }

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) generatedAt = LocalDateTime.now();
        if (status == null) status = ValuationLetterStatus.GENERATED;
    }
}
