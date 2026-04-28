package com.isec.platform.modules.integrations.quote.model;

import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftQuoteResponse {
    private PartnerType provider;
    private Long draftQuoteSysId;
    private String draftQuoteRef;
    private Long draftQuoteUserId;
    private BigDecimal draftQuoteAmount;
    private DraftQuoteStatus status;
    private Integer productId;
    private DraftQuoteInsuranceData insuranceData;
    private String clientName;
    private String clientPhone;
    private String clientEmail;
    private String clientIdNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private QuotePaymentSummary paymentSummary;
    private String rawResponse;
}
