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

import java.math.BigDecimal;
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
                .basicPremium(toBigDecimal(response.getBasicPremium()))
                .pvtBenefit(toBigDecimal(response.getPvtBenefit()))
                .excessProtectorBenefit(toBigDecimal(response.getExcessProtectorBenefit()))
                .windscreenBenefit(toBigDecimal(response.getWindscreenBenefit()))
                .radioCassetteBenefit(toBigDecimal(response.getRadioCassetteBenefit()))
                .lossOfUseBenefit(toBigDecimal(response.getLossOfUseBenefit()))
                .passengerLegalLiabilityBenefit(toBigDecimal(response.getPassengerLegalLiabilityBenefit()))
                .benefitsTotal(toBigDecimal(response.getBenefitsTotal()))
                .benefitsBreakdown(mapBenefitsBreakdown(response.getBenefitsBreakdown()))
                .grossPremiumBreakdown(mapGrossBreakdown(response.getGrossPremiumBreakdown()))
                .netPremium(toBigDecimal(response.getNetPremium()))
                .levies(toBigDecimal(response.getLevies()))
                .stampDuty(toBigDecimal(response.getStampDuty()))
                .grossPremium(toBigDecimal(response.getGrossPremium()))
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
                        .amount(toBigDecimal(item.getAmount()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<PremiumGrossBreakdown> mapGrossBreakdown(List<SanlamPremiumBreakdownItem> items) {
        return Optional.ofNullable(items)
                .orElse(Collections.emptyList())
                .stream()
                .map(item -> PremiumGrossBreakdown.builder()
                        .label(item.getLabel())
                        .amount(toBigDecimal(item.getAmount()))
                        .build())
                .collect(Collectors.toList());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            String strValue = (String) value;
            if ("Inclusive".equalsIgnoreCase(strValue)) {
                return BigDecimal.ZERO;
            }
            try {
                return new BigDecimal(strValue);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
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
