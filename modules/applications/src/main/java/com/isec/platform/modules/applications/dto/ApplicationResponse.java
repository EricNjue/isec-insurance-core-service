package com.isec.platform.modules.applications.dto;

import com.isec.platform.modules.applications.domain.ApplicationStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}
