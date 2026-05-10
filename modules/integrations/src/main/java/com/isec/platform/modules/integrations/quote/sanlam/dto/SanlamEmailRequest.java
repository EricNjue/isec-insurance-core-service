package com.isec.platform.modules.integrations.quote.sanlam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanlamEmailRequest {
    @JsonProperty("quot_sys_id")
    private Long quotSysId;
    @JsonProperty("include_receipt")
    private boolean includeReceipt;
    @JsonProperty("include_debit_note")
    private boolean includeDebitNote;
    @JsonProperty("recipient_email")
    private String recipientEmail;
}
