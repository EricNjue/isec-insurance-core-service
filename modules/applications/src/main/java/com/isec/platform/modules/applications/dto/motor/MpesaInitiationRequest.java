package com.isec.platform.modules.applications.dto.motor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.isec.platform.modules.applications.dto.QuoteRequest.KycDetails;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpesaInitiationRequest {
    private String phoneNumber; // Optional override
    private Double amount;
    private KycDetails kycDetails;
}
