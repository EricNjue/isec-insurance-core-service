package com.isec.platform.modules.integrations.quote.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteCoverDetails {
    private String bankInterest;
    private String bankName;
    private String valuer;
    private String physicalAddress;
    private LocalDate coverStartDate;
    private LocalDate coverEndDate;
}
