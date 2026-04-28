package com.isec.platform.modules.integrations.quote.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteDisclaimers {
    private boolean ownershipDeclaration;
    private boolean vehicleInspection;
    private boolean termsConditions;
    private boolean selfDeclaration;
}
