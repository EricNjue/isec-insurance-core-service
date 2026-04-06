package com.isec.platform.modules.certificates.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class ExtractedCertificateMetadata {
    private String partnerCode;
    private String policyNumber;
    private String certificateNumber;
    private String registrationNumber;
    private String chassisNumber;
    private String customerEmail;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private Double sumInsured;
}
