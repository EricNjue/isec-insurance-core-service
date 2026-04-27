package com.isec.platform.modules.certificates.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Table("certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {
    @Id
    private Long id;

    private Long policyId;

    private String dmvicReference;

    private CertificateType type;

    private CertificateStatus status;

    private String idempotencyKey;

    private String partnerCode;
    private String certificateNumber;
    private String policyNumber;
    private String vehicleRegistrationNumber;
    private String chassisNumber;
    private String customerEmail;
    private String s3Bucket;
    private String s3Key;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String checksum;
    private String ingestionSource;
    private String emailMessageId;
    private LocalDateTime emailReceivedAt;

    private LocalDate startDate;
    private LocalDate expiryDate;
    private LocalDateTime issuedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
