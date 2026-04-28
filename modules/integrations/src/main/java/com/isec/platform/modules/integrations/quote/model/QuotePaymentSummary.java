package com.isec.platform.modules.integrations.quote.model;

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
public class QuotePaymentSummary {
    private BigDecimal totalAmount;
    private BigDecimal totalPaid;
    private BigDecimal remainingBalance;
    private Integer installmentCount;
    private List<BigDecimal> installmentAmounts;
    private String status;
    private List<Object> transactions;
    private List<Object> installments;
}
