package com.isec.platform.modules.rating.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class RatingContext {
    private String tenantId;
    private String category;
    private BigDecimal vehicleValue;
    private Integer vehicleAge;
    private String vehicleMake;
    private String vehicleModel;
    private Set<Long> selectedAddonIds;
    private Map<String, Object> additionaldata;
}
