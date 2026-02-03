package com.isec.platform.modules.payments.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StkPushRequest {
    private Long applicationId;
    private BigDecimal amount;
    private String phoneNumber;
}
