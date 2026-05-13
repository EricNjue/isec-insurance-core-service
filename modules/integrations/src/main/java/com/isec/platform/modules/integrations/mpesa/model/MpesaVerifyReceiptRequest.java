package com.isec.platform.modules.integrations.mpesa.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpesaVerifyReceiptRequest {
    @JsonProperty("quote_ref")
    private String quoteRef;
    private String receipt;
    @JsonProperty("numberOf_installments")
    private Integer numberOfInstallments;
}
