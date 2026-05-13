package com.isec.platform.modules.applications.dto.motor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.isec.platform.modules.applications.domain.motor.MotorQuoteStatus;
import com.isec.platform.modules.applications.domain.motor.PaymentMethod;
import com.isec.platform.modules.applications.domain.motor.PaymentVerificationMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MotorPaymentResult {
    private PaymentMethod paymentMethod;
    private PaymentVerificationMode verificationMode;
    private MotorQuoteStatus status;
    private String providerReference;
    private String checkoutId;
    private String receipt;
    private BigDecimal amount;
    private String paidAt;
    private String businessNumber;
    private String accountNumber;
    private List<String> instructions;
    private String rawResponse;
    private Map<String, Object> metadata;
}
