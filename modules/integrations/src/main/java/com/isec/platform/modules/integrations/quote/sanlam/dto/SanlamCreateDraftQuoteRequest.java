package com.isec.platform.modules.integrations.quote.sanlam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanlamCreateDraftQuoteRequest {
    @JsonProperty("draft_quote_amount")
    private BigDecimal draftQuoteAmount;
    
    @JsonProperty("client_name")
    private String clientName;
    
    @JsonProperty("client_phone")
    private String clientPhone;
    
    @JsonProperty("client_email")
    private String clientEmail;
    
    @JsonProperty("client_id_number")
    private String clientIdNumber;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("insurance_data")
    private SanlamInsuranceData insuranceData;
    
    @JsonProperty("draft_quote_user_id")
    private Long draftQuoteUserId;
}
