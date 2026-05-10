package com.isec.platform.modules.integrations.quote.sanlam.mapper;

import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatusResponse;
import com.isec.platform.modules.integrations.quote.model.*;
import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import com.isec.platform.modules.integrations.quote.sanlam.dto.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SanlamDraftQuoteMapper {

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
                .vehicleType(data.getVehicleType())
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
        if (benefits == null || benefits.getItems() == null) return null;
        
        SanlamBenefits.SanlamBenefitsBuilder builder = SanlamBenefits.builder();
        
        if (benefits.getItems().containsKey("pvt")) {
            builder.pvt(mapToSanlamBenefit(benefits.getItems().get("pvt")));
        }
        if (benefits.getItems().containsKey("excess_protector")) {
            builder.excessProtector(mapToSanlamBenefit(benefits.getItems().get("excess_protector")));
        }
        if (benefits.getItems().containsKey("courtesy_car")) {
            builder.courtesyCar(mapToSanlamBenefit(benefits.getItems().get("courtesy_car")));
        }
        if (benefits.getItems().containsKey("windscreen")) {
            builder.windscreen(mapToSanlamWindscreen(benefits.getItems().get("windscreen")));
        }
        if (benefits.getItems().containsKey("radio_cassette")) {
            builder.radioCassette(mapToSanlamWindscreen(benefits.getItems().get("radio_cassette")));
        }
        if (benefits.getItems().containsKey("passenger_legal_liability")) {
            builder.passengerLegalLiability(mapToSanlamBenefit(benefits.getItems().get("passenger_legal_liability")));
        }
        
        return builder.build();
    }

    private SanlamBenefits.SanlamBenefit mapToSanlamBenefit(QuoteBenefitsDetails.BenefitItem item) {
        return SanlamBenefits.SanlamBenefit.builder()
                .benefit(item.getBenefit())
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

    private DraftQuoteStatus mapStatus(String status, SanlamPaymentSummary paymentSummary) {
        if (status == null) {
            return DraftQuoteStatus.UNKNOWN;
        }
        if ("draft".equalsIgnoreCase(status)) {
            if (paymentSummary != null && "pending".equalsIgnoreCase(paymentSummary.getStatus())) {
                return DraftQuoteStatus.PENDING_PAYMENT;
            }
            return DraftQuoteStatus.DRAFT;
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
                .benefit(item.getBenefit())
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

        QuotePaymentSummary summary = draftQuote.getPaymentSummary();
        BigDecimal amount = BigDecimal.valueOf(paymentStatus.getAmount() != null ? paymentStatus.getAmount() : 0.0);

        // Format phone number as E.164 (e.g. +254722129685)
        String phoneNumber = draftQuote.getClientPhone();
        if (phoneNumber != null) {
            phoneNumber = phoneNumber.trim();
            if (phoneNumber.startsWith("0")) {
                phoneNumber = "+254" + phoneNumber.substring(1);
            } else if (phoneNumber.startsWith("254") && !phoneNumber.startsWith("+")) {
                phoneNumber = "+" + phoneNumber;
            } else if (!phoneNumber.startsWith("+")) {
                phoneNumber = "+254" + phoneNumber;
            }
        }

        // Format paid_at as ISO 8601 with timezone
        String paidAt = paymentStatus.getPaidAt();
        try {
            if (paidAt != null) {
                OffsetDateTime odt = OffsetDateTime.parse(paidAt, DateTimeFormatter.ISO_DATE_TIME);
                paidAt = odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } else {
                paidAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }
        } catch (Exception e) {
            paidAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }

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
                                .numberOfInstallments(summary != null ? summary.getInstallmentCount() : 1)
                                .paymentContext("initial")
                                .build())
                        .build())
                .build();
    }

    public PolicyIssuanceResult toPolicyIssuanceResult(SanlamDraftQuoteResponse response, SanlamEmailResponse emailResponse) {
        if (response == null) return null;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("quot_sys_id", response.getQuotSysId());
        metadata.put("draft_quote_sys_id", response.getDraftQuoteSysId());

        return PolicyIssuanceResult.builder()
                .status("POLICY_ISSUED")
                .message(emailResponse != null ? emailResponse.getMessage() : "Policy updated successfully")
                .policyReference(String.valueOf(response.getQuotSysId()))
                .externalReference(response.getDraftQuoteRef())
                .emailSent(emailResponse != null)
                .metadata(metadata)
                .build();
    }
}
