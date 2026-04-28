package com.isec.platform.modules.integrations.mpesa.sanlam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanlamStkPushRequest {
    @JsonProperty("quote_ref")
    private String quoteRef;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("amount")
    private Double amount;
}
