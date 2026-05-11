package com.isec.platform.modules.integrations.quote.sanlam.mapper;

import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatusResponse;
import com.isec.platform.modules.integrations.quote.model.*;
import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import com.isec.platform.modules.integrations.quote.sanlam.config.SanlamQuoteProperties;
import com.isec.platform.modules.integrations.quote.sanlam.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SanlamDraftQuoteMapper {

    private final SanlamQuoteProperties properties;

    public SanlamCreateDraftQuoteRequest toSanlamRequest(DraftQuoteRequest request) {
        if (request == null) return null;

        return SanlamCreateDraftQuoteRequest.builder()
                .draftQuoteAmount(request.getDraftQuoteAmount())
                .clientName(request.getClientName())
                .clientPhone(request.getClientPhone())
                .clientEmail(request.getClientEmail())
                .clientIdNumber(request.getClientIdNumber())
                .status(request.getStatus())
                .draftQuoteUserId(request.getDraftQuoteUserId())
                .insuranceData(toSanlamInsuranceData(request.getInsuranceData()))
                .build();
    }

    private SanlamInsuranceData toSanlamInsuranceData(DraftQuoteInsuranceData data) {
        if (data == null) return null;

        return SanlamInsuranceData.builder()
                .rateEngine(toSanlamRateEngine(data.getPremium(), data.getCover()))
                .vehicle(toSanlamVehicle(data.getVehicle()))
                .premium(toSanlamPremium(data.getPremium()))
                .premiums(toSanlamPremiums(data.getPremium()))
                .benefits(toSanlamBenefits(data.getBenefits()))
                .subclass(data.getSubclass())
                .vehicleType("premier_auto") // TODO: Replace hardcoded Sanlam integration defaults with canonical/UI-driven values once E2E integration is stable.
                .status(data.getStatus())
                .client(toSanlamClient(data.getClient()))
                .cover(toSanlamCover(data.getCover()))
                .disclaimers(toSanlamDisclaimers(data.getDisclaimers()))
                .dmvicCheck(toSanlamDmvicCheck(data.getDmvicCheck()))
                .submittedAt(data.getSubmittedAt())
                .build();
    }

    private SanlamRateEngine toSanlamRateEngine(QuotePremiumDetails premium, QuoteCoverDetails cover) {
        if (premium == null || premium.getRateSetUsed() == null) return null;

        return SanlamRateEngine.builder()
                .baseRateSetId(premium.getBaseRateSetId() != null ? premium.getBaseRateSetId().intValue() : null)
                .specialRateSetId(null)
                .rateSetUsed(premium.getRateSetUsed())
                .asOfDate(cover != null ? cover.getCoverStartDate() : null)
                .calculationDetails(SanlamRateEngine.CalculationDetails.builder()
                        .baseRateSetId(premium.getBaseRateSetId() != null ? premium.getBaseRateSetId().intValue() : null)
                        .baseRateSetName(premium.getBaseRateSetName())
                        .specialRateApplied(premium.isSpecialRateApplied())
                        .pvtInclusiveApplicable(premium.isPvtInclusiveApplicable())
                        .excessProtectorInclusiveApplicable(premium.isExcessProtectorInclusiveApplicable())
                        .build())
                .build();
    }

    private SanlamVehicle toSanlamVehicle(QuoteVehicleDetails vehicle) {
        if (vehicle == null) return null;
        return SanlamVehicle.builder()
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .yearOfManufacture(vehicle.getYearOfManufacture())
                .sumInsured(vehicle.getSumInsured())
                .registrationNumber(vehicle.getRegistrationNumber())
                .registration(vehicle.getRegistrationNumber())
                .year(vehicle.getYearOfManufacture())
                .value(vehicle.getSumInsured())
                .bodyType("002") // TODO: Replace hardcoded Sanlam integration defaults with canonical/UI-driven values once E2E integration is stable.
                .chassisNumber(vehicle.getChassisNumber())
                .engineNumber(vehicle.getEngineNumber())
                .seatingCapacity("7") // TODO: Replace hardcoded Sanlam integration defaults with canonical/UI-driven values once E2E integration is stable.
                .tonnage("2") // TODO: Replace hardcoded Sanlam integration defaults with canonical/UI-driven values once E2E integration is stable.
                .numberOfPassengers("7") // TODO: Replace hardcoded Sanlam integration defaults with canonical/UI-driven values once E2E integration is stable.
                .cc("3000") // TODO: Replace hardcoded Sanlam integration defaults with canonical/UI-driven values once E2E integration is stable.
                .motorClass("private") // TODO: Replace hardcoded Sanlam integration defaults with canonical/UI-driven values once E2E integration is stable.
                .vehicleClass("C") // TODO: Replace hardcoded Sanlam integration defaults with canonical/UI-driven values once E2E integration is stable.
                .build();
    }

    private SanlamPremium toSanlamPremium(QuotePremiumDetails premium) {
        if (premium == null) return null;
        return SanlamPremium.builder()
                .basicPremium(premium.getBasicPremium())
                .grossPremium(premium.getGrossPremium())
                .sumInsured(premium.getSumInsured())
                .build();
    }

    private SanlamPremiums toSanlamPremiums(QuotePremiumDetails premium) {
        if (premium == null) return null;
        return SanlamPremiums.builder()
                .basic(premium.getBasicPremium())
                .gross(premium.getGrossPremium())
                .net(premium.getNetPremium())
                .levies(premium.getLevies())
                .stampDuty(premium.getStampDuty())
                .build();
    }

    private SanlamBenefits toSanlamBenefits(QuoteBenefitsDetails benefits) {
        // TODO: Replace hardcoded Sanlam integration defaults with canonical/UI-driven values once E2E integration is stable.
        return SanlamBenefits.builder()
                .pvt(SanlamBenefits.SanlamBenefit.builder()
                        .benefit(new BigDecimal("17000"))
                        .interest("yes")
                        .build())
                .excessProtector(SanlamBenefits.SanlamBenefit.builder()
                        .benefit("Inclusive")
                        .interest("yes")
                        .build())
                .courtesyCar(SanlamBenefits.SanlamBenefit.builder()
                        .benefit(new BigDecimal("7500"))
                        .interest("yes")
                        .days("10")
                        .build())
                .windscreen(SanlamBenefits.SanlamWindscreenBenefit.builder()
                        .benefit(BigDecimal.ZERO)
                        .extraBenefit(BigDecimal.ZERO)
                        .clientAdditionalAmount(BigDecimal.ZERO)
                        .build())
                .radioCassette(SanlamBenefits.SanlamWindscreenBenefit.builder()
                        .benefit(BigDecimal.ZERO)
                        .extraBenefit(BigDecimal.ZERO)
                        .clientAdditionalAmount(BigDecimal.ZERO)
                        .build())
                .passengerLegalLiability(SanlamBenefits.SanlamBenefit.builder()
                        .benefit(BigDecimal.ZERO)
                        .interest("no")
                        .build())
                .build();
    }

    private SanlamBenefits.SanlamBenefit mapToSanlamBenefit(QuoteBenefitsDetails.BenefitItem item) {
        return SanlamBenefits.SanlamBenefit.builder()
                .benefit((Object) item.getBenefit())
                .interest(item.getInterest())
                .days(item.getDays())
                .build();
    }

    private SanlamBenefits.SanlamWindscreenBenefit mapToSanlamWindscreen(QuoteBenefitsDetails.BenefitItem item) {
        return SanlamBenefits.SanlamWindscreenBenefit.builder()
                .benefit(item.getBenefit())
                .extraBenefit(item.getExtraBenefit())
                .clientAdditionalAmount(item.getClientAdditionalAmount())
                .build();
    }

    private SanlamClient toSanlamClient(QuoteClientDetails client) {
        if (client == null) return null;
        return SanlamClient.builder()
                .type(client.getType())
                .name(client.getName())
                .phone(client.getPhone())
                .email(client.getEmail())
                .idNumber(client.getIdNumber())
                .kraPin(client.getKraPin())
                .city(client.getCity())
                .postalAddress(client.getPostalAddress())
                .dateOfBirth(client.getDateOfBirth())
                .gender(client.getGender())
                .build();
    }

    private SanlamCover toSanlamCover(QuoteCoverDetails cover) {
        if (cover == null) return null;
        return SanlamCover.builder()
                .bankInterest(cover.getBankInterest())
                .bankName(cover.getBankName())
                .valuer(cover.getValuer())
                .physicalAddress(cover.getPhysicalAddress())
                .coverStartDate(cover.getCoverStartDate())
                .coverEndDate(cover.getCoverEndDate())
                .build();
    }

    private SanlamDisclaimers toSanlamDisclaimers(QuoteDisclaimers disclaimers) {
        // Hardcoded for now as per requirement
        // TODO: Map from disclaimers object when available in common model
        return SanlamDisclaimers.builder()
                .ownershipDeclaration(true)
                .vehicleInspection(true)
                .termsConditions(true)
                .selfDeclaration(true)
                .build();
    }

    private SanlamDmvicCheck toSanlamDmvicCheck(QuoteDmvicCheck check) {
        if (check == null) return null;

        // If no active cover found, ensure exact portal-style payload
        if ("clear".equalsIgnoreCase(check.getStatus()) && "No active cover found".equalsIgnoreCase(check.getMessage())) {
            return SanlamDmvicCheck.builder()
                    .status("clear")
                    .message("No active cover found")
                    .evidence(Map.of("status", "clear"))
                    .checkedAt(check.getCheckedAt())
                    .transactionRef(null)
                    .hasDoubleInsurance(false)
                    .build();
        }

        return SanlamDmvicCheck.builder()
                .checkedAt(check.getCheckedAt())
                .hasDoubleInsurance(check.isHasDoubleInsurance())
                .status(check.getStatus())
                .transactionRef(check.getTransactionRef())
                .message(check.getMessage())
                .evidence(check.getEvidence())
                .build();
    }

    public DraftQuoteResponse toCommonResponse(SanlamDraftQuoteResponse sanlamResponse) {
        if (sanlamResponse == null) return null;

        return DraftQuoteResponse.builder()
                .provider(PartnerType.SANLAM)
                .draftQuoteSysId(sanlamResponse.getDraftQuoteSysId())
                .draftQuoteRef(sanlamResponse.getDraftQuoteRef())
                .draftQuoteUserId(sanlamResponse.getDraftQuoteUserId())
                .draftQuoteAmount(sanlamResponse.getDraftQuoteAmount())
                .status(mapStatus(sanlamResponse.getStatus(), sanlamResponse.getPaymentSummary()))
                .productId(sanlamResponse.getProductId())
                .insuranceData(toCommonInsuranceData(sanlamResponse.getInsuranceData()))
                .clientName(sanlamResponse.getClientName())
                .clientPhone(sanlamResponse.getClientPhone())
                .clientEmail(sanlamResponse.getClientEmail())
                .clientIdNumber(sanlamResponse.getClientIdNumber())
                .createdAt(sanlamResponse.getCreatedAt())
                .updatedAt(sanlamResponse.getUpdatedAt())
                .paymentSummary(toCommonPaymentSummary(sanlamResponse.getPaymentSummary()))
                .build();
    }

    public DraftQuoteStatus mapStatus(String status, SanlamPaymentSummary paymentSummary) {
        if (status == null) {
            return DraftQuoteStatus.UNKNOWN;
        }
        if ("draft".equalsIgnoreCase(status)) {
            if (paymentSummary != null && "pending".equalsIgnoreCase(paymentSummary.getStatus())) {
                return DraftQuoteStatus.PENDING_PAYMENT;
            }
            return DraftQuoteStatus.DRAFT;
        }
        if ("valuation_pending".equalsIgnoreCase(status)) {
            return DraftQuoteStatus.VALUATION_PENDING;
        }
        try {
            return DraftQuoteStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            return DraftQuoteStatus.UNKNOWN;
        }
    }

    private DraftQuoteInsuranceData toCommonInsuranceData(SanlamInsuranceData data) {
        if (data == null) return null;

        return DraftQuoteInsuranceData.builder()
                .vehicle(toCommonVehicle(data.getVehicle()))
                .premium(toCommonPremium(data.getPremium(), data.getPremiums()))
                .benefits(toCommonBenefits(data.getBenefits()))
                .cover(toCommonCover(data.getCover()))
                .client(toCommonClient(data.getClient()))
                .disclaimers(toCommonDisclaimers(data.getDisclaimers()))
                .dmvicCheck(toCommonDmvicCheck(data.getDmvicCheck()))
                .subclass(data.getSubclass())
                .vehicleType(data.getVehicleType())
                .status(data.getStatus())
                .submittedAt(data.getSubmittedAt())
                .build();
    }

    private QuoteVehicleDetails toCommonVehicle(SanlamVehicle vehicle) {
        if (vehicle == null) return null;
        return QuoteVehicleDetails.builder()
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .yearOfManufacture(vehicle.getYearOfManufacture())
                .sumInsured(vehicle.getSumInsured())
                .registrationNumber(vehicle.getRegistrationNumber())
                .bodyType(vehicle.getBodyType())
                .chassisNumber(vehicle.getChassisNumber())
                .engineNumber(vehicle.getEngineNumber())
                .seatingCapacity(vehicle.getSeatingCapacity())
                .tonnage(vehicle.getTonnage())
                .cc(vehicle.getCc())
                .motorClass(vehicle.getMotorClass())
                .vehicleClass(vehicle.getVehicleClass())
                .build();
    }

    private QuotePremiumDetails toCommonPremium(SanlamPremium premium, SanlamPremiums premiums) {
        QuotePremiumDetails.QuotePremiumDetailsBuilder builder = QuotePremiumDetails.builder();
        if (premium != null) {
            builder.basicPremium(premium.getBasicPremium())
                    .grossPremium(premium.getGrossPremium())
                    .sumInsured(premium.getSumInsured());
        }
        if (premiums != null) {
            builder.basicPremium(premiums.getBasic())
                    .grossPremium(premiums.getGross())
                    .netPremium(premiums.getNet())
                    .levies(premiums.getLevies())
                    .stampDuty(premiums.getStampDuty());
        }
        return builder.build();
    }

    private QuoteBenefitsDetails toCommonBenefits(SanlamBenefits benefits) {
        if (benefits == null) return null;
        Map<String, QuoteBenefitsDetails.BenefitItem> items = new HashMap<>();

        if (benefits.getPvt() != null) {
            items.put("pvt", toCommonBenefit(benefits.getPvt()));
        }
        if (benefits.getExcessProtector() != null) {
            items.put("excess_protector", toCommonBenefit(benefits.getExcessProtector()));
        }
        if (benefits.getCourtesyCar() != null) {
            items.put("courtesy_car", toCommonBenefit(benefits.getCourtesyCar()));
        }
        if (benefits.getWindscreen() != null) {
            items.put("windscreen", toCommonWindscreen(benefits.getWindscreen()));
        }
        if (benefits.getRadioCassette() != null) {
            items.put("radio_cassette", toCommonWindscreen(benefits.getRadioCassette()));
        }
        if (benefits.getPassengerLegalLiability() != null) {
            items.put("passenger_legal_liability", toCommonBenefit(benefits.getPassengerLegalLiability()));
        }

        return QuoteBenefitsDetails.builder().items(items).build();
    }

    private QuoteBenefitsDetails.BenefitItem toCommonBenefit(SanlamBenefits.SanlamBenefit item) {
        return QuoteBenefitsDetails.BenefitItem.builder()
                .benefit(item.getBenefit() instanceof BigDecimal ? (BigDecimal) item.getBenefit() : null)
                .interest(item.getInterest())
                .days(item.getDays())
                .build();
    }

    private QuoteBenefitsDetails.BenefitItem toCommonWindscreen(SanlamBenefits.SanlamWindscreenBenefit item) {
        return QuoteBenefitsDetails.BenefitItem.builder()
                .benefit(item.getBenefit())
                .extraBenefit(item.getExtraBenefit())
                .clientAdditionalAmount(item.getClientAdditionalAmount())
                .build();
    }

    private QuoteClientDetails toCommonClient(SanlamClient client) {
        if (client == null) return null;
        return QuoteClientDetails.builder()
                .type(client.getType())
                .name(client.getName())
                .phone(client.getPhone())
                .email(client.getEmail())
                .idNumber(client.getIdNumber())
                .kraPin(client.getKraPin())
                .city(client.getCity())
                .postalAddress(client.getPostalAddress())
                .dateOfBirth(client.getDateOfBirth())
                .gender(client.getGender())
                .build();
    }

    private QuoteCoverDetails toCommonCover(SanlamCover cover) {
        if (cover == null) return null;
        return QuoteCoverDetails.builder()
                .bankInterest(cover.getBankInterest())
                .bankName(cover.getBankName())
                .valuer(cover.getValuer())
                .physicalAddress(cover.getPhysicalAddress())
                .coverStartDate(cover.getCoverStartDate())
                .coverEndDate(cover.getCoverEndDate())
                .build();
    }

    private QuoteDisclaimers toCommonDisclaimers(SanlamDisclaimers disclaimers) {
        if (disclaimers == null) return null;
        return QuoteDisclaimers.builder()
                .ownershipDeclaration(disclaimers.isOwnershipDeclaration())
                .vehicleInspection(disclaimers.isVehicleInspection())
                .termsConditions(disclaimers.isTermsConditions())
                .selfDeclaration(disclaimers.isSelfDeclaration())
                .build();
    }

    private QuoteDmvicCheck toCommonDmvicCheck(SanlamDmvicCheck check) {
        if (check == null) return null;
        return QuoteDmvicCheck.builder()
                .checkedAt(check.getCheckedAt())
                .hasDoubleInsurance(check.isHasDoubleInsurance())
                .status(check.getStatus())
                .transactionRef(check.getTransactionRef())
                .message(check.getMessage())
                .evidence(check.getEvidence())
                .build();
    }

    private QuotePaymentSummary toCommonPaymentSummary(SanlamPaymentSummary summary) {
        if (summary == null) return null;
        return QuotePaymentSummary.builder()
                .totalAmount(summary.getTotalAmount())
                .totalPaid(summary.getTotalPaid())
                .remainingBalance(summary.getRemainingBalance())
                .installmentCount(summary.getInstallmentCount())
                .installmentAmounts(summary.getInstallmentAmounts())
                .status(summary.getStatus())
                .transactions(summary.getTransactions())
                .installments(summary.getInstallments())
                .build();
    }

    public SanlamUpdateDraftQuoteRequest toUpdateDraftQuoteRequest(DraftQuoteResponse draftQuote, MpesaPaymentStatusResponse paymentStatus) {
        if (draftQuote == null || paymentStatus == null) return null;

        BigDecimal amount = draftQuote.getDraftQuoteAmount();
        String paidAt = paymentStatus.getPaidAt();
        String phoneNumber = draftQuote.getClientPhone();

        return SanlamUpdateDraftQuoteRequest.builder()
                .insuranceData(SanlamUpdateDraftQuoteRequest.InsuranceData.builder()
                        .payment(SanlamUpdateDraftQuoteRequest.PaymentData.builder()
                                .method("stk")
                                .status("success")
                                .checkoutId(paymentStatus.getCheckoutId())
                                .receipt(paymentStatus.getReceiptNumber())
                                .amount(amount)
                                .paidAt(paidAt)
                                .phoneNumber(phoneNumber)
                                .installmentNumber(1)
                                .numberOfInstallments(1)
                                .build())
                        .build())
                .build();
    }


    public PolicyIssuanceResult toPolicyIssuanceResult(SanlamDraftQuoteResponse response, SanlamEmailResponse emailResponse) {
        if (response == null) return null;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("quot_sys_id", response.getQuotSysId());
        metadata.put("draft_quote_sys_id", response.getDraftQuoteSysId());
        metadata.put("draft_quote_ref", response.getDraftQuoteRef());
        metadata.put("status", response.getStatus());

        return PolicyIssuanceResult.builder()
                .status("POLICY_ISSUED")
                .message(emailResponse != null ? emailResponse.getMessage() : "Policy updated successfully")
                .policyReference(String.valueOf(response.getQuotSysId()))
                .externalReference(response.getDraftQuoteRef())
                .emailSent(emailResponse != null)
                .metadata(metadata)
                .build();
    }

    public SanlamDraftQuoteResponse toSanlamDraftQuoteResponse(DraftQuoteResponse response) {
        if (response == null) return null;
        return SanlamDraftQuoteResponse.builder()
                .draftQuoteSysId(response.getDraftQuoteSysId())
                .draftQuoteRef(response.getDraftQuoteRef())
                .draftQuoteUserId(response.getDraftQuoteUserId())
                .draftQuoteAmount(response.getDraftQuoteAmount())
                .status(response.getStatus() != null ? response.getStatus().name() : null)
                .productId(response.getProductId())
                .clientName(response.getClientName())
                .clientPhone(response.getClientPhone())
                .clientEmail(response.getClientEmail())
                .clientIdNumber(response.getClientIdNumber())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .paymentSummary(toSanlamPaymentSummary(response.getPaymentSummary()))
                .build();
    }

    private SanlamPaymentSummary toSanlamPaymentSummary(QuotePaymentSummary summary) {
        if (summary == null) return null;
        return SanlamPaymentSummary.builder()
                .totalAmount(summary.getTotalAmount())
                .totalPaid(summary.getTotalPaid())
                .remainingBalance(summary.getRemainingBalance())
                .installmentCount(summary.getInstallmentCount())
                .installmentAmounts(summary.getInstallmentAmounts())
                .status(summary.getStatus())
                .transactions(summary.getTransactions())
                .installments(summary.getInstallments())
                .build();
    }
}
