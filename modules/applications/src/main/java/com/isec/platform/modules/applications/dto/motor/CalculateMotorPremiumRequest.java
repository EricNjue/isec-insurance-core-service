package com.isec.platform.modules.applications.dto.motor;

import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckResponse;
import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import com.isec.platform.modules.applications.dto.QuoteRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateMotorPremiumRequest {
    @NotNull
    private String quoteId;
    @NotNull
    private PartnerType partner;
    @Valid
    @NotNull
    private QuoteRequest.InsuranceDetails insuranceDetails;
    @Valid
    @NotNull
    private QuoteRequest.VehicleDetails vehicleDetails;
    @Valid
    private QuoteRequest.KycDetails kycDetails;
}
