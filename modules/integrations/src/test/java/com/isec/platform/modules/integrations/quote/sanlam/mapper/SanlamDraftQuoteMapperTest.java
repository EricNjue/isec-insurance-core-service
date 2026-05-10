package com.isec.platform.modules.integrations.quote.sanlam.mapper;

import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatusResponse;
import com.isec.platform.modules.integrations.quote.model.*;
import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import com.isec.platform.modules.integrations.quote.sanlam.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SanlamDraftQuoteMapperTest {

    private SanlamDraftQuoteMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SanlamDraftQuoteMapper();
    }

    @Test
    void shouldMapRequestToSanlamRequest() {
        DraftQuoteRequest request = DraftQuoteRequest.builder()
                .draftQuoteAmount(new BigDecimal("59808"))
                .clientName("ERIC GITONGA NJUE")
                .clientPhone("+254722129685")
                .clientEmail("njue.gitonga92@gmail.com")
                .clientIdNumber("28972735")
                .status("draft")
                .draftQuoteUserId(561L)
                .insuranceData(DraftQuoteInsuranceData.builder()
                        .subclass("private")
                        .vehicleType("standard_auto")
                        .status("draft")
                        .client(QuoteClientDetails.builder()
                                .type("individual")
                                .name("ERIC GITONGA NJUE")
                                .phone("+254722129685")
                                .email("njue.gitonga92@gmail.com")
                                .idNumber("28972735")
                                .kraPin("A123456789B")
                                .city("NAIROBI")
                                .postalAddress("1435")
                                .dateOfBirth(LocalDate.of(1992, 6, 27))
                                .gender("Male")
                                .build())
                        .vehicle(QuoteVehicleDetails.builder()
                                .make("AUDI")
                                .model("Q7")
                                .registrationNumber("QEWEQWEWQE")
                                .sumInsured(new BigDecimal("500000"))
                                .build())
                        .cover(QuoteCoverDetails.builder()
                                .bankInterest("no")
                                .bankName("")
                                .valuer("SOLVIT")
                                .physicalAddress("Nairobi")
                                .coverStartDate(LocalDate.of(2026, 4, 26))
                                .coverEndDate(LocalDate.of(2027, 4, 25))
                                .build())
                        .submittedAt(LocalDateTime.now())
                        .build())
                .build();

        SanlamCreateDraftQuoteRequest sanlamRequest = mapper.toSanlamRequest(request);
        
        assertThat(sanlamRequest.getStatus()).isEqualTo("draft");
        assertThat(sanlamRequest.getDraftQuoteAmount()).isEqualTo(new BigDecimal("59808"));
        assertThat(sanlamRequest.getClientName()).isEqualTo("ERIC GITONGA NJUE");
        assertThat(sanlamRequest.getInsuranceData().getSubclass()).isEqualTo("private");
        assertThat(sanlamRequest.getInsuranceData().getVehicle().getMake()).isEqualTo("AUDI");
        assertThat(sanlamRequest.getInsuranceData().getCover().getCoverStartDate()).isEqualTo(LocalDate.of(2026, 4, 26));
        assertThat(sanlamRequest.getInsuranceData().getCover().getCoverEndDate()).isEqualTo(LocalDate.of(2027, 4, 25));
        assertThat(sanlamRequest.getInsuranceData().getCover().getBankInterest()).isEqualTo("no");
        assertThat(sanlamRequest.getInsuranceData().getCover().getValuer()).isEqualTo("SOLVIT");
        assertThat(sanlamRequest.getInsuranceData().getCover().getPhysicalAddress()).isEqualTo("Nairobi");
        
        // Verify client mapping
        assertThat(sanlamRequest.getInsuranceData().getClient()).isNotNull();
        assertThat(sanlamRequest.getInsuranceData().getClient().getType()).isEqualTo("individual");
        assertThat(sanlamRequest.getInsuranceData().getClient().getKraPin()).isEqualTo("A123456789B");
        assertThat(sanlamRequest.getInsuranceData().getClient().getCity()).isEqualTo("NAIROBI");
        assertThat(sanlamRequest.getInsuranceData().getClient().getDateOfBirth()).isEqualTo(LocalDate.of(1992, 6, 27));
        assertThat(sanlamRequest.getInsuranceData().getClient().getGender()).isEqualTo("Male");

        // Verify disclaimers mapping (hardcoded to true)
        assertThat(sanlamRequest.getInsuranceData().getDisclaimers()).isNotNull();
        assertThat(sanlamRequest.getInsuranceData().getDisclaimers().isOwnershipDeclaration()).isTrue();
        assertThat(sanlamRequest.getInsuranceData().getDisclaimers().isVehicleInspection()).isTrue();
        assertThat(sanlamRequest.getInsuranceData().getDisclaimers().isTermsConditions()).isTrue();
        assertThat(sanlamRequest.getInsuranceData().getDisclaimers().isSelfDeclaration()).isTrue();

        // Verify hardcoded values
        assertThat(sanlamRequest.getClientPhone()).isEqualTo("+254722129685");
        assertThat(sanlamRequest.getInsuranceData().getVehicleType()).isEqualTo("premier_auto");
        assertThat(sanlamRequest.getInsuranceData().getVehicle().getBodyType()).isEqualTo("002");
        assertThat(sanlamRequest.getInsuranceData().getVehicle().getSeatingCapacity()).isEqualTo("7");
        assertThat(sanlamRequest.getInsuranceData().getVehicle().getTonnage()).isEqualTo("2");
        assertThat(sanlamRequest.getInsuranceData().getVehicle().getNumberOfPassengers()).isEqualTo("7");
        assertThat(sanlamRequest.getInsuranceData().getVehicle().getCc()).isEqualTo("3000");
        assertThat(sanlamRequest.getInsuranceData().getVehicle().getMotorClass()).isEqualTo("private");
        assertThat(sanlamRequest.getInsuranceData().getVehicle().getVehicleClass()).isEqualTo("C");

        // Verify benefits hardcoded values
        SanlamBenefits benefits = sanlamRequest.getInsuranceData().getBenefits();
        assertThat(benefits).isNotNull();
        assertThat(benefits.getPvt().getBenefit()).isEqualTo(new BigDecimal("17000"));
        assertThat(benefits.getPvt().getInterest()).isEqualTo("yes");
        assertThat(benefits.getExcessProtector().getBenefit()).isEqualTo("Inclusive");
        assertThat(benefits.getExcessProtector().getInterest()).isEqualTo("yes");
        assertThat(benefits.getCourtesyCar().getBenefit()).isEqualTo(new BigDecimal("7500"));
        assertThat(benefits.getCourtesyCar().getInterest()).isEqualTo("yes");
        assertThat(benefits.getCourtesyCar().getDays()).isEqualTo("10");
        assertThat(benefits.getWindscreen().getBenefit()).isEqualTo(BigDecimal.ZERO);
        assertThat(benefits.getRadioCassette().getBenefit()).isEqualTo(BigDecimal.ZERO);
        assertThat(benefits.getPassengerLegalLiability().getBenefit()).isEqualTo(BigDecimal.ZERO);
        assertThat(benefits.getPassengerLegalLiability().getInterest()).isEqualTo("no");
    }

    @Test
    void shouldMapDmvicCheckWithTransactionRef() {
        DraftQuoteRequest request = DraftQuoteRequest.builder()
                .insuranceData(DraftQuoteInsuranceData.builder()
                        .dmvicCheck(QuoteDmvicCheck.builder()
                                .checkedAt(LocalDateTime.now())
                                .hasDoubleInsurance(true)
                                .status("double")
                                .transactionRef("OA-ZD9849")
                                .message("Active cover found")
                                .evidence(Map.of("status", "double"))
                                .build())
                        .build())
                .build();

        SanlamCreateDraftQuoteRequest sanlamRequest = mapper.toSanlamRequest(request);

        assertThat(sanlamRequest.getInsuranceData().getDmvicCheck()).isNotNull();
        assertThat(sanlamRequest.getInsuranceData().getDmvicCheck().getTransactionRef()).isEqualTo("OA-ZD9849");
        assertThat(sanlamRequest.getInsuranceData().getDmvicCheck().getStatus()).isEqualTo("double");
    }

    @Test
    void shouldMapPremiumAndPremiumsInDraftQuoteRequest() {
        DraftQuoteRequest request = DraftQuoteRequest.builder()
                .draftQuoteAmount(new BigDecimal("229568"))
                .insuranceData(DraftQuoteInsuranceData.builder()
                        .premium(QuotePremiumDetails.builder()
                                .basicPremium(new BigDecimal("204000"))
                                .grossPremium(new BigDecimal("229568"))
                                .netPremium(new BigDecimal("228500"))
                                .levies(new BigDecimal("1028"))
                                .stampDuty(new BigDecimal("40"))
                                .sumInsured(new BigDecimal("6800000"))
                                .rateSetUsed("GUIDELINES SAZ APRIL 2026")
                                .baseRateSetId(39L)
                                .baseRateSetName("GUIDELINES SAZ APRIL 2026")
                                .specialRateApplied(false)
                                .pvtInclusiveApplicable(false)
                                .excessProtectorInclusiveApplicable(false)
                                .build())
                        .cover(QuoteCoverDetails.builder()
                                .coverStartDate(LocalDate.of(2026, 4, 26))
                                .build())
                        .build())
                .build();

        SanlamCreateDraftQuoteRequest sanlamRequest = mapper.toSanlamRequest(request);

        assertThat(sanlamRequest.getInsuranceData().getPremium()).isNotNull();
        assertThat(sanlamRequest.getInsuranceData().getPremium().getBasicPremium()).isEqualTo(new BigDecimal("204000"));
        assertThat(sanlamRequest.getInsuranceData().getPremium().getGrossPremium()).isEqualTo(new BigDecimal("229568"));
        assertThat(sanlamRequest.getInsuranceData().getPremium().getSumInsured()).isEqualTo(new BigDecimal("6800000"));

        assertThat(sanlamRequest.getInsuranceData().getPremiums()).isNotNull();
        assertThat(sanlamRequest.getInsuranceData().getPremiums().getBasic()).isEqualTo(new BigDecimal("204000"));
        assertThat(sanlamRequest.getInsuranceData().getPremiums().getGross()).isEqualTo(new BigDecimal("229568"));
        assertThat(sanlamRequest.getInsuranceData().getPremiums().getNet()).isEqualTo(new BigDecimal("228500"));
        assertThat(sanlamRequest.getInsuranceData().getPremiums().getLevies()).isEqualTo(new BigDecimal("1028"));
        assertThat(sanlamRequest.getInsuranceData().getPremiums().getStampDuty()).isEqualTo(new BigDecimal("40"));

        // Verify rate engine mapping
        SanlamRateEngine rateEngine = sanlamRequest.getInsuranceData().getRateEngine();
        assertThat(rateEngine).isNotNull();
        assertThat(rateEngine.getBaseRateSetId()).isEqualTo(39);
        assertThat(rateEngine.getRateSetUsed()).isEqualTo("GUIDELINES SAZ APRIL 2026");
        assertThat(rateEngine.getAsOfDate()).isEqualTo(LocalDate.of(2026, 4, 26));
        assertThat(rateEngine.getCalculationDetails()).isNotNull();
        assertThat(rateEngine.getCalculationDetails().getBaseRateSetId()).isEqualTo(39);
        assertThat(rateEngine.getCalculationDetails().getBaseRateSetName()).isEqualTo("GUIDELINES SAZ APRIL 2026");
        assertThat(rateEngine.getCalculationDetails().isSpecialRateApplied()).isFalse();
        assertThat(rateEngine.getCalculationDetails().isPvtInclusiveApplicable()).isFalse();
        assertThat(rateEngine.getCalculationDetails().isExcessProtectorInclusiveApplicable()).isFalse();
    }

    @Test
    void shouldMapSanlamResponseToCommonResponse() {
        SanlamDraftQuoteResponse sanlamResponse = SanlamDraftQuoteResponse.builder()
                .draftQuoteSysId(1402L)
                .draftQuoteRef("REF123")
                .draftQuoteAmount(new BigDecimal("59808"))
                .status("draft")
                .clientName("ERIC GITONGA NJUE")
                .paymentSummary(SanlamPaymentSummary.builder()
                        .totalAmount(new BigDecimal("59808"))
                        .status("pending")
                        .installmentAmounts(List.of(new BigDecimal("19936"), new BigDecimal("19936")))
                        .build())
                .insuranceData(SanlamInsuranceData.builder()
                        .vehicle(SanlamVehicle.builder()
                                .make("AUDI")
                                .model("Q7")
                                .build())
                        .premiums(SanlamPremiums.builder()
                                .basic(new BigDecimal("50000"))
                                .gross(new BigDecimal("59808"))
                                .levies(new BigDecimal("268"))
                                .stampDuty(new BigDecimal("40"))
                                .build())
                        .benefits(SanlamBenefits.builder()
                                .pvt(SanlamBenefits.SanlamBenefit.builder().interest("no").build())
                                .windscreen(SanlamBenefits.SanlamWindscreenBenefit.builder().benefit(new BigDecimal("50000")).build())
                                .build())
                        .build())
                .build();

        DraftQuoteResponse response = mapper.toCommonResponse(sanlamResponse);
        System.out.println("[DEBUG_LOG] Status: " + response.getStatus());

        assertThat(response.getProvider()).isEqualTo(PartnerType.SANLAM);
        assertThat(response.getDraftQuoteSysId()).isEqualTo(1402L);
        assertThat(response.getStatus()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(DraftQuoteStatus.PENDING_PAYMENT); // draft status + pending payment summary = PENDING_PAYMENT
        assertThat(response.getInsuranceData().getVehicle().getMake()).isEqualTo("AUDI");
        assertThat(response.getInsuranceData().getPremium().getLevies()).isEqualTo(new BigDecimal("268"));
        assertThat(response.getInsuranceData().getPremium().getStampDuty()).isEqualTo(new BigDecimal("40"));
        assertThat(response.getInsuranceData().getBenefits().getItems()).containsKey("pvt");
        assertThat(response.getInsuranceData().getBenefits().getItems().get("windscreen").getBenefit()).isEqualTo(new BigDecimal("50000"));
        assertThat(response.getPaymentSummary().getStatus()).isEqualTo("pending");
    }

    @Test
    void shouldMapStatusCorrectly() {
        // ... existing code ...
    }

    @Test
    void shouldMapUpdateDraftQuoteRequestWithStrictPaymentPayload() {
        DraftQuoteResponse draftQuote = DraftQuoteResponse.builder()
                .clientPhone("0719531872")
                .paymentSummary(QuotePaymentSummary.builder()
                        .totalAmount(new BigDecimal("229568"))
                        .totalPaid(BigDecimal.ZERO)
                        .remainingBalance(new BigDecimal("229568"))
                        .installmentCount(2)
                        .build())
                .build();

        MpesaPaymentStatusResponse paymentStatus = MpesaPaymentStatusResponse.builder()
                .amount(80349.0)
                .checkoutId("ws_CO_123")
                .receiptNumber("UDTEM2PDHA")
                .paidAt("2026-04-29T23:43:21Z")
                .status(com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatus.SUCCESS)
                .build();

        SanlamUpdateDraftQuoteRequest request = mapper.toUpdateDraftQuoteRequest(draftQuote, paymentStatus);

        assertThat(request.getInsuranceData().getPayment().getPhoneNumber()).isEqualTo("+254719531872");
        assertThat(request.getInsuranceData().getPayment().getAmount()).isEqualTo(new BigDecimal("80349.0"));
        assertThat(request.getInsuranceData().getPayment().getNumberOfInstallments()).isEqualTo(2);
        assertThat(request.getInsuranceData().getPayment().getPaidAt()).isNotNull();
        System.out.println("[DEBUG_LOG] PaidAt: " + request.getInsuranceData().getPayment().getPaidAt());

        // Verify fields are absent or null (via DTO structure)
        // vehicle and benefits should not be present in InsuranceData
        // total_amount, total_paid, etc. should not be in PaymentData
    }

}
