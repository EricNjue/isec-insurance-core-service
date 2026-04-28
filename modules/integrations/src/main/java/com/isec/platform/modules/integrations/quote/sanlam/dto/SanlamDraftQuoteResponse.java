package com.isec.platform.modules.integrations.quote.sanlam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class SanlamDraftQuoteResponse {
    @JsonProperty("draft_quote_sys_id")
    private Long draftQuoteSysId;
    
    @JsonProperty("draft_quote_ref")
    private String draftQuoteRef;
    
    @JsonProperty("draft_quote_user_id")
    private Long draftQuoteUserId;
    
    @JsonProperty("draft_quote_amount")
    private BigDecimal draftQuoteAmount;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("product_id")
    private Integer productId;
    
    @JsonProperty("insurance_data")
    private SanlamInsuranceData insuranceData;
    
    @JsonProperty("client_name")
    private String clientName;
    
    @JsonProperty("client_phone")
    private String clientPhone;
    
    @JsonProperty("client_email")
    private String clientEmail;
    
    @JsonProperty("client_id_number")
    private String clientIdNumber;
    
    @JsonProperty("motor_class")
    private String motorClass;
    
    @JsonProperty("make_model")
    private String makeModel;
    
    @JsonProperty("vehicle_make")
    private String vehicleMake;
    
    @JsonProperty("vehicle_model")
    private String vehicleModel;
    
    @JsonProperty("year_of_manufacture")
    private Integer yearOfManufacture;
    
    @JsonProperty("vehicle_value")
    private BigDecimal vehicleValue;
    
    @JsonProperty("registration_number")
    private String registrationNumber;
    
    @JsonProperty("quot_sys_id")
    private Long quotSysId;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("created_by")
    private String createdBy;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("updated_by")
    private String updatedBy;
    
    @JsonProperty("agent_name")
    private String agentName;
    
    @JsonProperty("payment_summary")
    private SanlamPaymentSummary paymentSummary;
}
