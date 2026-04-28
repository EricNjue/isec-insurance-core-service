package com.isec.platform.modules.integrations.quote.sanlam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class SanlamPaymentSummary {
    @JsonProperty("total_amount")
    private BigDecimal totalAmount;
    
    @JsonProperty("total_paid")
    private BigDecimal totalPaid;
    
    @JsonProperty("remaining_balance")
    private BigDecimal remainingBalance;
    
    @JsonProperty("installment_count")
    private Integer installmentCount;
    
    @JsonProperty("installment_amounts")
    private List<BigDecimal> installmentAmounts;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("transactions")
    private List<Object> transactions;
    
    @JsonProperty("installments")
    private List<Object> installments;
}
