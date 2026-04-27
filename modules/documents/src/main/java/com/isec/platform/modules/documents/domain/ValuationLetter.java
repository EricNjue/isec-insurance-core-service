package com.isec.platform.modules.documents.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDateTime;

@Table("valuation_letters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValuationLetter {
    @Id
    private Long id;

    private Long policyId;

    private Long certificateId;

    private String vehicleRegistrationNumber;

    private String insuredName;

    private String policyNumber;

    private String pdfS3Bucket;

    private String pdfS3Key;

    private String generatedBy;

    private LocalDateTime generatedAt;

    private ValuationLetterStatus status;

    private java.util.UUID documentUuid;

    private String documentHash;

    private String documentType;

    private String verificationMetadata;

    public enum ValuationLetterStatus {
        ACTIVE, REVOKED, EXPIRED, GENERATED, SENT
    }
}
