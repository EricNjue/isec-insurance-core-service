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
public class SanlamClient {
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("phone")
    private String phone;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("id_number")
    private String idNumber;
    
    @JsonProperty("kra_pin")
    private String kraPin;
    
    @JsonProperty("city")
    private String city;
    
    @JsonProperty("postal_address")
    private String postalAddress;
    
    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;
    
    @JsonProperty("gender")
    private String gender;
}
