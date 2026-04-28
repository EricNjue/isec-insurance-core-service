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
public class SanlamStkStatusRequest {
    @JsonProperty("quote_ref")
    private String quoteRef;

    @JsonProperty("checkout_id")
    private String checkoutId;
}
