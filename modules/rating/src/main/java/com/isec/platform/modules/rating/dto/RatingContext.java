package com.isec.platform.modules.rating.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class RatingContext {
    private String tenantId;
    private String category;
    private BigDecimal vehicleValue;
    private Integer vehicleAge;
    private String vehicleMake;
    private String vehicleModel;
    private Map<String, Object> additionaldata;
}
