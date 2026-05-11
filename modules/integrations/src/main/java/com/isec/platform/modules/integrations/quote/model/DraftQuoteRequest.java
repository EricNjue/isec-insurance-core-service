package com.isec.platform.modules.integrations.quote.model;

import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftQuoteRequest {
    private PartnerType provider;
    private BigDecimal draftQuoteAmount;
    private String clientName;
    private String clientPhone;
    private String clientEmail;
    private String clientIdNumber;
    private String status;
    private DraftQuoteInsuranceData insuranceData;
    private Long draftQuoteUserId;
    private Long draftQuoteSysId;
    private String draftQuoteRef;
    private Map<String, Object> metadata;
}
