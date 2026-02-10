package com.isec.platform.modules.applications.dto;

import com.isec.platform.modules.rating.dto.PricingResult;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class QuoteResponse {
    private String quoteId;
    private String tenantId;
    private String category;
    private String vehicleMake;
    private String vehicleModel;
    private Integer yearOfManufacture;
    private BigDecimal vehicleValue;

    private Long rateBookId;
    private String rateBookVersion;
    private String cacheKey;

    private PricingResult pricing;
    private LocalDateTime expiryDate;
}
