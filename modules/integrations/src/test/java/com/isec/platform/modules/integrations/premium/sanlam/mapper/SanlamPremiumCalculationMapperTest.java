package com.isec.platform.modules.integrations.premium.sanlam.mapper;

import com.isec.platform.modules.integrations.premium.model.PremiumCalculationRequest;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationResponse;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationStatus;
import com.isec.platform.modules.integrations.premium.provider.PremiumProviderType;
import com.isec.platform.modules.integrations.premium.sanlam.dto.SanlamCalculatePremiumRequest;
import com.isec.platform.modules.integrations.premium.sanlam.dto.SanlamCalculatePremiumResponse;
import com.isec.platform.modules.integrations.premium.sanlam.dto.SanlamCalculationDetails;
import com.isec.platform.modules.integrations.premium.sanlam.dto.SanlamPremiumBreakdownItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SanlamPremiumCalculationMapperTest {

    private SanlamPremiumCalculationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SanlamPremiumCalculationMapper();
    }

    @Test
    void shouldMapRequestToSanlamRequest() {
        PremiumCalculationRequest request = PremiumCalculationRequest.builder()
                .rateType("new_business")
                .vehicleValue(new BigDecimal("500000"))
                .vehicleMake("AUDI")
                .vehicleModel("Q7")
                .vehicleYear(2018)
                .motorClass("private")
                .pvtInterest("no")
                .excessProtectorInterest("yes")
                .windscreenBenefit(new BigDecimal("50000"))
                .radioCassetteBenefit(new BigDecimal("50000"))
                .lossOfUseDays(0)
                .passengerLegalLiability("None")
                .build();

        SanlamCalculatePremiumRequest sanlamRequest = mapper.toSanlamRequest(request);

        assertThat(sanlamRequest.getRateType()).isEqualTo("new_business");
        assertThat(sanlamRequest.getVehicleValue()).isEqualTo(new BigDecimal("500000"));
        assertThat(sanlamRequest.getVehicleMake()).isEqualTo("AUDI");
        assertThat(sanlamRequest.getVehicleModel()).isEqualTo("Q7");
        assertThat(sanlamRequest.getVehicleYear()).isEqualTo(2018);
        assertThat(sanlamRequest.getMotorClass()).isEqualTo("private");
        assertThat(sanlamRequest.getPvtInterest()).isEqualTo("no");
        assertThat(sanlamRequest.getExcessProtectorInterest()).isEqualTo("yes");
        assertThat(sanlamRequest.getWindscreenBenefit()).isEqualTo(new BigDecimal("50000"));
        assertThat(sanlamRequest.getRadioCassetteBenefit()).isEqualTo(new BigDecimal("50000"));
        assertThat(sanlamRequest.getLossOfUseDays()).isZero();
        assertThat(sanlamRequest.getPassengerLegalLiability()).isEqualTo("None");
    }

    @Test
    void shouldMapSanlamResponseToCommonResponse() {
        SanlamCalculatePremiumResponse sanlamResponse = SanlamCalculatePremiumResponse.builder()
                .basicPremium(new BigDecimal("50000"))
                .pvtBenefit(BigDecimal.ZERO)
                .excessProtectorBenefit(new BigDecimal("5000"))
                .benefitsTotal(new BigDecimal("5000"))
                .netPremium(new BigDecimal("55000"))
                .levies(new BigDecimal("248"))
                .stampDuty(new BigDecimal("40"))
                .grossPremium(new BigDecimal("55288"))
                .rateSetUsed("GUIDELINES SAZ APRIL 2026")
                .specialRateApplied(false)
                .productSystemId(2)
                .productCode("1002")
                .benefitsBreakdown(List.of(
                        new SanlamPremiumBreakdownItem("PVT benefit", BigDecimal.ZERO),
                        new SanlamPremiumBreakdownItem("Excess protector", new BigDecimal("5000"))
                ))
                .grossPremiumBreakdown(List.of(
                        new SanlamPremiumBreakdownItem("Net premium", new BigDecimal("55000")),
                        new SanlamPremiumBreakdownItem("Gross premium", new BigDecimal("55288"))
                ))
                .calculationDetails(SanlamCalculationDetails.builder()
                        .baseRateSetId(39L)
                        .baseRateSetName("GUIDELINES SAZ APRIL 2026")
                        .build())
                .build();

        PremiumCalculationResponse response = mapper.toCommonResponse(sanlamResponse);

        assertThat(response.getProvider()).isEqualTo(PremiumProviderType.SANLAM);
        assertThat(response.getStatus()).isEqualTo(PremiumCalculationStatus.SUCCESS);
        assertThat(response.getBasicPremium()).isEqualTo(new BigDecimal("50000"));
        assertThat(response.getBenefitsTotal()).isEqualTo(new BigDecimal("5000"));
        assertThat(response.getGrossPremium()).isEqualTo(new BigDecimal("55288"));
        assertThat(response.getBenefitsBreakdown()).hasSize(2);
        assertThat(response.getBenefitsBreakdown().get(0).getLabel()).isEqualTo("PVT benefit");
        assertThat(response.getGrossPremiumBreakdown()).hasSize(2);
        assertThat(response.getCalculationMetadata().getBaseRateSetId()).isEqualTo(39L);
        assertThat(response.getProductCode()).isEqualTo("1002");
    }

    @Test
    void shouldReturnFailedStatusWhenSanlamResponseIsNull() {
        PremiumCalculationResponse response = mapper.toCommonResponse(null);
        assertThat(response.getStatus()).isEqualTo(PremiumCalculationStatus.FAILED);
        assertThat(response.getProvider()).isEqualTo(PremiumProviderType.SANLAM);
    }
}
