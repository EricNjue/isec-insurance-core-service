package com.isec.platform.modules.payments.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentRequest {
    private UUID policyId;
    private BigDecimal amount;
    private String currency;
}
