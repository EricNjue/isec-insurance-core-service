package com.isec.platform.modules.applications.dto;

import com.isec.platform.modules.applications.domain.ApplicationStatus;
import com.isec.platform.modules.documents.dto.ApplicationDocumentDto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApplicationResponse {
    private Long id;
    private String userId;
    private String registrationNumber;
    private String vehicleMake;
    private String vehicleModel;
    private Integer yearOfManufacture;
    private BigDecimal vehicleValue;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
    private List<ApplicationDocumentDto> documents;
}
