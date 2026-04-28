package com.isec.platform.modules.integrations.quote.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftQuoteInsuranceData {
    private QuoteVehicleDetails vehicle;
    private QuotePremiumDetails premium;
    private QuoteBenefitsDetails benefits;
    private QuoteCoverDetails cover;
    private QuoteClientDetails client;
    private QuoteDisclaimers disclaimers;
    private QuoteDmvicCheck dmvicCheck;
    private String subclass;
    private String vehicleType;
    private String status;
    private LocalDateTime submittedAt;
}
