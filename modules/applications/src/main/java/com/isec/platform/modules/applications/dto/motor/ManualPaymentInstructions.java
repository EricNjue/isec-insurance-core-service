package com.isec.platform.modules.applications.dto.motor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.isec.platform.modules.applications.domain.motor.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManualPaymentInstructions {
    private PaymentMethod paymentMethod;
    private String businessNumber;
    private String accountNumber;
    private BigDecimal amount;
    private String currency;
    private List<String> instructions;
}
