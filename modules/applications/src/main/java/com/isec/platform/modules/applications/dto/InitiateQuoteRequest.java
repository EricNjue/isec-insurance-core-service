package com.isec.platform.modules.applications.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateQuoteRequest {
    
    @NotBlank
    private String licensePlateNumber;
    private String chassisNumber;
}
