package com.isec.platform.modules.integrations.sanlam.mpesa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpesaPaymentStatus {
    private PaymentStatus status;
    private String message;
    private String receiptNumber;
    private Double amount;
    private String paidAt;
    private Object rawResponse;

    public enum PaymentStatus {
        SUCCESS,
        FAILED,
        PENDING
    }
}
