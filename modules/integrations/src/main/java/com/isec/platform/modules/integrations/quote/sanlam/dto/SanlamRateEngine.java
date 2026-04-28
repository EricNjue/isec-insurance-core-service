package com.isec.platform.modules.integrations.quote.sanlam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanlamRateEngine {
    @JsonProperty("base_rate_set_id")
    private Integer baseRateSetId;
    
    @JsonProperty("special_rate_set_id")
    private Integer specialRateSetId;
    
    @JsonProperty("rate_set_used")
    private String rateSetUsed;
    
    @JsonProperty("as_of_date")
    private LocalDate asOfDate;
    
    @JsonProperty("calculation_details")
    private CalculationDetails calculationDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationDetails {
        private Integer baseRateSetId;
        private String baseRateSetName;
        private boolean specialRateApplied;
        private boolean pvtInclusiveApplicable;
        private boolean excessProtectorInclusiveApplicable;
    }
}
