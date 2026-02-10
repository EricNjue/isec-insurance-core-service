package com.isec.platform.modules.applications.dto;

import com.isec.platform.modules.rating.dto.PricingResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteResponse {
    private String quoteId;
    private String tenantId;
    private String category;
    private String vehicleMake;
    private String vehicleModel;
    private Integer yearOfManufacture;
    private BigDecimal vehicleValue;
    private String registrationNumber;
    private String chassisNumber;
    private String engineNumber;

    private Long rateBookId;
    private String rateBookVersion;
    private String cacheKey;

    private PricingResult pricing;
    private LocalDateTime expiryDate;
}
