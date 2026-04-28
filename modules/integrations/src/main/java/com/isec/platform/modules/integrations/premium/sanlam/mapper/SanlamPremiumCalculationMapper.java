package com.isec.platform.modules.integrations.premium.sanlam.mapper;

import com.isec.platform.modules.integrations.premium.model.PremiumBenefitBreakdown;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationMetadata;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationRequest;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationResponse;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationStatus;
import com.isec.platform.modules.integrations.premium.model.PremiumGrossBreakdown;
import com.isec.platform.modules.integrations.premium.provider.PremiumProviderType;
import com.isec.platform.modules.integrations.premium.sanlam.dto.SanlamCalculatePremiumRequest;
import com.isec.platform.modules.integrations.premium.sanlam.dto.SanlamCalculatePremiumResponse;
import com.isec.platform.modules.integrations.premium.sanlam.dto.SanlamCalculationDetails;
import com.isec.platform.modules.integrations.premium.sanlam.dto.SanlamPremiumBreakdownItem;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SanlamPremiumCalculationMapper {

    public SanlamCalculatePremiumRequest toSanlamRequest(PremiumCalculationRequest request) {
        if (request == null) {
            return null;
        }

        return SanlamCalculatePremiumRequest.builder()
                .rateType(request.getRateType())
                .vehicleValue(request.getVehicleValue())
                .vehicleMake(request.getVehicleMake())
                .vehicleModel(request.getVehicleModel())
                .vehicleYear(request.getVehicleYear())
                .motorClass(request.getMotorClass())
                .motorSubclass(request.getMotorSubclass())
                .pvtInterest(request.getPvtInterest())
                .excessProtectorInterest(request.getExcessProtectorInterest())
                .windscreenBenefit(request.getWindscreenBenefit())
                .radioCassetteBenefit(request.getRadioCassetteBenefit())
                .lossOfUseDays(request.getLossOfUseDays())
                .passengerLegalLiability(request.getPassengerLegalLiability())
                .clientIdentifier(request.getClientIdentifier())
                .build();
    }

    public PremiumCalculationResponse toCommonResponse(SanlamCalculatePremiumResponse response) {
        if (response == null) {
            return PremiumCalculationResponse.builder()
                    .provider(PremiumProviderType.SANLAM)
                    .status(PremiumCalculationStatus.FAILED)
                    .build();
        }

        return PremiumCalculationResponse.builder()
                .provider(PremiumProviderType.SANLAM)
                .status(PremiumCalculationStatus.SUCCESS)
                .basicPremium(response.getBasicPremium())
                .pvtBenefit(response.getPvtBenefit())
                .excessProtectorBenefit(response.getExcessProtectorBenefit())
                .windscreenBenefit(response.getWindscreenBenefit())
                .radioCassetteBenefit(response.getRadioCassetteBenefit())
                .lossOfUseBenefit(response.getLossOfUseBenefit())
                .passengerLegalLiabilityBenefit(response.getPassengerLegalLiabilityBenefit())
                .benefitsTotal(response.getBenefitsTotal())
                .benefitsBreakdown(mapBenefitsBreakdown(response.getBenefitsBreakdown()))
                .grossPremiumBreakdown(mapGrossBreakdown(response.getGrossPremiumBreakdown()))
                .netPremium(response.getNetPremium())
                .levies(response.getLevies())
                .stampDuty(response.getStampDuty())
                .grossPremium(response.getGrossPremium())
                .rateSetUsed(response.getRateSetUsed())
                .specialRateApplied(response.isSpecialRateApplied())
                .productSystemId(response.getProductSystemId())
                .productCode(response.getProductCode())
                .calculationMetadata(mapMetadata(response.getCalculationDetails()))
                .build();
    }

    private List<PremiumBenefitBreakdown> mapBenefitsBreakdown(List<SanlamPremiumBreakdownItem> items) {
        return Optional.ofNullable(items)
                .orElse(Collections.emptyList())
                .stream()
                .map(item -> PremiumBenefitBreakdown.builder()
                        .label(item.getLabel())
                        .amount(item.getAmount())
                        .build())
                .collect(Collectors.toList());
    }

    private List<PremiumGrossBreakdown> mapGrossBreakdown(List<SanlamPremiumBreakdownItem> items) {
        return Optional.ofNullable(items)
                .orElse(Collections.emptyList())
                .stream()
                .map(item -> PremiumGrossBreakdown.builder()
                        .label(item.getLabel())
                        .amount(item.getAmount())
                        .build())
                .collect(Collectors.toList());
    }

    private PremiumCalculationMetadata mapMetadata(SanlamCalculationDetails details) {
        if (details == null) {
            return null;
        }

        return PremiumCalculationMetadata.builder()
                .baseRateSetId(details.getBaseRateSetId())
                .baseRateSetName(details.getBaseRateSetName())
                .specialRateApplied(details.isSpecialRateApplied())
                .pvtInclusiveApplicable(details.isPvtInclusiveApplicable())
                .excessProtectorInclusiveApplicable(details.isExcessProtectorInclusiveApplicable())
                .build();
    }
}
