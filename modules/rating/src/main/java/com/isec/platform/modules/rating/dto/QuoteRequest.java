package com.isec.platform.modules.rating.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class QuoteRequest {
    private Long applicationId;
    private BigDecimal baseRate;
}
