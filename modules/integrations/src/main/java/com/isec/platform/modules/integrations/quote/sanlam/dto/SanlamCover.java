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
public class SanlamCover {
    @JsonProperty("bank_interest")
    private String bankInterest;
    
    @JsonProperty("bank_name")
    private String bankName;
    
    @JsonProperty("valuer")
    private String valuer;
    
    @JsonProperty("physical_address")
    private String physicalAddress;
    
    @JsonProperty("cover_start_date")
    private LocalDate coverStartDate;
    
    @JsonProperty("cover_end_date")
    private LocalDate coverEndDate;
}
