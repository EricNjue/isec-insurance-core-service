package com.isec.platform.modules.integrations.quote.sanlam.mapper;

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
                        .vehicle(QuoteVehicleDetails.builder()
                                .make("AUDI")
                                .model("Q7")
                                .registrationNumber("QEWEQWEWQE")
                                .sumInsured(new BigDecimal("500000"))
                                .build())
                        .cover(QuoteCoverDetails.builder()
                                .coverStartDate(LocalDate.of(2026, 4, 26))
                                .coverEndDate(LocalDate.of(2027, 4, 25))
                                .build())
                        .build())
                .build();

        SanlamCreateDraftQuoteRequest sanlamRequest = mapper.toSanlamRequest(request);

        assertThat(sanlamRequest.getDraftQuoteAmount()).isEqualTo(new BigDecimal("59808"));
        assertThat(sanlamRequest.getClientName()).isEqualTo("ERIC GITONGA NJUE");
        assertThat(sanlamRequest.getInsuranceData().getSubclass()).isEqualTo("private");
        assertThat(sanlamRequest.getInsuranceData().getVehicle().getMake()).isEqualTo("AUDI");
        assertThat(sanlamRequest.getInsuranceData().getCover().getCoverStartDate()).isEqualTo(LocalDate.of(2026, 4, 26));
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
        // Only draft status
        SanlamDraftQuoteResponse resp1 = SanlamDraftQuoteResponse.builder().status("draft").build();
        DraftQuoteResponse commonResp1 = mapper.toCommonResponse(resp1);
        assertThat(commonResp1.getStatus()).isEqualTo(DraftQuoteStatus.DRAFT);

        // Draft status with pending payment
        SanlamDraftQuoteResponse resp2 = SanlamDraftQuoteResponse.builder()
                .status("draft")
                .paymentSummary(SanlamPaymentSummary.builder().status("pending").build())
                .build();
        DraftQuoteResponse commonResp2 = mapper.toCommonResponse(resp2);
        assertThat(commonResp2.getStatus()).isEqualTo(DraftQuoteStatus.PENDING_PAYMENT);

        // Other statuses
        SanlamDraftQuoteResponse resp3 = SanlamDraftQuoteResponse.builder().status("paid").build();
        DraftQuoteResponse commonResp3 = mapper.toCommonResponse(resp3);
        assertThat(commonResp3.getStatus()).isEqualTo(DraftQuoteStatus.PAID);
    }
}
