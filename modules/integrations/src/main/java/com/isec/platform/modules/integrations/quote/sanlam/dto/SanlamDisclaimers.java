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
public class SanlamDisclaimers {
    @JsonProperty("ownership_declaration")
    private boolean ownershipDeclaration;
    
    @JsonProperty("vehicle_inspection")
    private boolean vehicleInspection;
    
    @JsonProperty("terms_conditions")
    private boolean termsConditions;
    
    @JsonProperty("self_declaration")
    private boolean selfDeclaration;
}
