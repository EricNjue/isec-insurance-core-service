package com.isec.platform.modules.certificates.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_partner_certificate", columnNames = {"partner_code", "certificate_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "policy_id")
    private Long policyId;

    @Column(unique = true, name = "dmvic_reference")
    private String dmvicReference;

    @Enumerated(EnumType.STRING)
    private CertificateType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateStatus status;

    @Column(unique = true, name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "partner_code")
    private String partnerCode;
    @Column(name = "certificate_number")
    private String certificateNumber;
    @Column(name = "policy_number")
    private String policyNumber;
    @Column(name = "vehicle_registration_number")
    private String vehicleRegistrationNumber;
    @Column(name = "chassis_number")
    private String chassisNumber;
    @Column(name = "customer_email")
    private String customerEmail;
    @Column(name = "s3_bucket")
    private String s3Bucket;
    @Column(name = "s3_key")
    private String s3Key;
    @Column(name = "file_name")
    private String fileName;
    @Column(name = "file_size")
    private Long fileSize;
    @Column(name = "content_type")
    private String contentType;
    private String checksum;
    @Column(name = "ingestion_source")
    private String ingestionSource;
    @Column(name = "email_message_id")
    private String emailMessageId;
    @Column(name = "email_received_at")
    private LocalDateTime emailReceivedAt;

    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
