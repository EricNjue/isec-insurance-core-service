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
public class QuoteClientDetails {
    private String type;
    private String name;
    private String phone;
    private String email;
    private String idNumber;
    private String kraPin;
    private String city;
    private String postalAddress;
    private LocalDate dateOfBirth;
    private String gender;
}
