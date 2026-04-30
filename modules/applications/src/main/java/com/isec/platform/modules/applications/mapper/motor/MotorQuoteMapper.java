package com.isec.platform.modules.applications.mapper.motor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.modules.applications.domain.motor.MotorQuoteApplication;
import com.isec.platform.modules.applications.domain.motor.MotorQuoteStatus;
import com.isec.platform.modules.applications.dto.QuoteRequest;
import com.isec.platform.modules.applications.dto.motor.CalculateMotorPremiumRequest;
import com.isec.platform.modules.applications.dto.motor.MotorQuoteResponse;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckResponse;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatusResponse;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationRequest;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationResponse;
import com.isec.platform.modules.integrations.premium.provider.PremiumProviderType;
import com.isec.platform.modules.integrations.quote.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MotorQuoteMapper {

    private final ObjectMapper objectMapper;

    public MotorQuoteApplication toEntity(CalculateMotorPremiumRequest request) {
        try {
            return MotorQuoteApplication.builder()
                    .quoteId(request.getQuoteId())
                    .partner(request.getPartner())
                    .status(MotorQuoteStatus.STARTED)
                    .insuranceDetails(objectMapper.writeValueAsString(request.getInsuranceDetails()))
                    .vehicleDetails(objectMapper.writeValueAsString(request.getVehicleDetails()))
                    .kycDetails(request.getKycDetails() != null ? objectMapper.writeValueAsString(request.getKycDetails()) : null)
                    .build();
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to serialize request details");
        }
    }

    public void updateEntity(MotorQuoteApplication entity, CalculateMotorPremiumRequest request) {
        try {
            entity.setPartner(request.getPartner());
            entity.setInsuranceDetails(objectMapper.writeValueAsString(request.getInsuranceDetails()));
            entity.setVehicleDetails(objectMapper.writeValueAsString(request.getVehicleDetails()));
            if (request.getKycDetails() != null) {
                entity.setKycDetails(objectMapper.writeValueAsString(request.getKycDetails()));
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to serialize request details");
        }
    }

    public void updateKycDetails(MotorQuoteApplication entity, QuoteRequest.KycDetails kycDetails) {
        if (kycDetails == null) return;
        try {
            entity.setKycDetails(objectMapper.writeValueAsString(kycDetails));
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to serialize KYC details");
        }
    }

    public PremiumCalculationRequest toPremiumRequest(MotorQuoteApplication app) {
        QuoteRequest.InsuranceDetails insurance = deserialize(app.getInsuranceDetails(), QuoteRequest.InsuranceDetails.class);
        QuoteRequest.VehicleDetails vehicle = deserialize(app.getVehicleDetails(), QuoteRequest.VehicleDetails.class);

        PremiumCalculationRequest.PremiumCalculationRequestBuilder builder = PremiumCalculationRequest.builder()
                .provider(PremiumProviderType.valueOf(app.getPartner().name()))
                .vehicleValue(vehicle.getValuationAmount())
                .vehicleMake(vehicle.getMakeCode())
                .vehicleModel(vehicle.getModelCode())
                .vehicleYear(vehicle.getYearOfManufacture())
                .motorClass(vehicle.getUsageType()) // Map usageType to motorClass
                .rateType("new_business");

        // Map addons
        if (insurance.getAddonRuleIds() != null) {
            // Simplified addon mapping logic as per requirements
            // todo :- we have a table called addon_definitions, we can use that to map the addon ids to addon names, it has to be partner agnostic, we need a way to achieve this, even if it means modifying the table structure
            if (insurance.getAddonRuleIds().contains(1L)) builder.excessProtectorInterest("yes");
            if (insurance.getAddonRuleIds().contains(2L)) builder.pvtInterest("yes");
            // Add more as needed or use a more sophisticated mapping layer
        }

        if (insurance.getAdditionalData() != null) {
            Object courtesyDays = insurance.getAdditionalData().get("courtesyCarDays");
            if (courtesyDays instanceof Number) {
                builder.lossOfUseDays(((Number) courtesyDays).intValue());
            }
        }

        return builder.build();
    }

    public DraftQuoteRequest toDraftQuoteRequest(MotorQuoteApplication app) {
        QuoteRequest.InsuranceDetails insurance = deserialize(app.getInsuranceDetails(), QuoteRequest.InsuranceDetails.class);
        QuoteRequest.VehicleDetails vehicle = deserialize(app.getVehicleDetails(), QuoteRequest.VehicleDetails.class);
        QuoteRequest.KycDetails kyc = deserialize(app.getKycDetails(), QuoteRequest.KycDetails.class);
        PremiumCalculationResponse premium = deserialize(app.getPremiumResult(), PremiumCalculationResponse.class);

        LocalDate startDate = LocalDate.now().plusDays(1);
        if (insurance.getInsuranceStartDate() != null) {
            startDate = LocalDate.parse(insurance.getInsuranceStartDate(), DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        }

        QuoteCoverDetails cover = QuoteCoverDetails.builder()
                .bankInterest("no")
                .bankName("")
                .valuer("SOLVIT") // TODO: Hardcoded for now
                .physicalAddress("Nairobi") // TODO: Hardcoded for now
                .coverStartDate(startDate)
                .coverEndDate(startDate.plusYears(1).minusDays(1))
                .build();

        QuoteDmvicCheck dmvicCheck = null;
        if (app.getDmvicCheckResult() != null) {
            DoubleInsuranceCheckResponse check = deserialize(app.getDmvicCheckResult(), DoubleInsuranceCheckResponse.class);
            dmvicCheck = QuoteDmvicCheck.builder()
                    .checkedAt(LocalDateTime.now()) // We don't have the original checkedAt, use now
                    .hasDoubleInsurance(check.isHasDuplicate())
                    .status(check.getStatus())
                    .transactionRef(check.getTransactionRef() != null ? check.getTransactionRef() :
                            (check.getDetails() != null ? "TXN-" + check.getDetails().getCertificateNumber() : "N/A"))
                    .message(check.getMessage())
                    .evidence(check.getDetails() != null ? objectMapper.convertValue(check.getDetails(), Map.class) : new HashMap<>())
                    .build();
        }

        return DraftQuoteRequest.builder()
                .provider(app.getPartner())
                .draftQuoteAmount(premium.getGrossPremium())
                .clientName(kyc.getFullName())
                .clientPhone(kyc.getPhoneNumber())
                .clientEmail(kyc.getEmail())
                .clientIdNumber(kyc.getIdNumber() != null ? kyc.getIdNumber() : "N/A")
                .status("draft")
                .draftQuoteUserId(561L) // todo:- Default, need to know how this value shd be derived
                .insuranceData(DraftQuoteInsuranceData.builder()
                        .subclass("private")
                        .vehicleType("standard_auto")
                        .status("draft")
                        .client(QuoteClientDetails.builder()
                                .type("individual")
                                .name(kyc.getFullName())
                                .phone(kyc.getPhoneNumber())
                                .email(kyc.getEmail())
                                .idNumber(kyc.getIdNumber())
                                .kraPin(kyc.getKraPin())
                                .city(kyc.getCity())
                                .postalAddress(kyc.getPostalAddress())
                                .dateOfBirth(kyc.getDateOfBirth())
                                .gender(kyc.getGender())
                                .build())
                        .vehicle(QuoteVehicleDetails.builder()
                                .make(vehicle.getMakeCode())
                                .model(vehicle.getModelCode())
                                .yearOfManufacture(vehicle.getYearOfManufacture())
                                .sumInsured(vehicle.getValuationAmount())
                                .registrationNumber(vehicle.getLicensePlateNumber())
                                .chassisNumber(vehicle.getChassisNumber())
                                .engineNumber(vehicle.getEngineNumber())
                                .build())
                        .premium(QuotePremiumDetails.builder()
                                .basicPremium(premium.getBasicPremium())
                                .grossPremium(premium.getGrossPremium())
                                .netPremium(premium.getNetPremium())
                                .levies(premium.getLevies())
                                .stampDuty(premium.getStampDuty())
                                .sumInsured(vehicle.getValuationAmount())
                                .rateSetUsed(premium.getRateSetUsed())
                                .baseRateSetId(premium.getCalculationMetadata() != null ? premium.getCalculationMetadata().getBaseRateSetId() : null)
                                .baseRateSetName(premium.getCalculationMetadata() != null ? premium.getCalculationMetadata().getBaseRateSetName() : null)
                                .specialRateApplied(premium.isSpecialRateApplied())
                                .pvtInclusiveApplicable(premium.getCalculationMetadata() != null && premium.getCalculationMetadata().isPvtInclusiveApplicable())
                                .excessProtectorInclusiveApplicable(premium.getCalculationMetadata() != null && premium.getCalculationMetadata().isExcessProtectorInclusiveApplicable())
                                .build())
                        .cover(cover)
                        .dmvicCheck(dmvicCheck)
                        .submittedAt(LocalDateTime.now())
                        .build())
                .build();
    }

    public MotorQuoteResponse toResponse(MotorQuoteApplication app) {
        MotorQuoteResponse.MotorQuoteResponseBuilder builder = MotorQuoteResponse.builder()
                .quoteId(app.getQuoteId())
                .partner(app.getPartner())
                .status(app.getStatus())
                .insuranceDetails(deserialize(app.getInsuranceDetails(), QuoteRequest.InsuranceDetails.class))
                .vehicleDetails(deserialize(app.getVehicleDetails(), QuoteRequest.VehicleDetails.class))
                .kycDetails(deserialize(app.getKycDetails(), QuoteRequest.KycDetails.class));

        if (app.getPremiumResult() != null) {
            PremiumCalculationResponse premium = deserialize(app.getPremiumResult(), PremiumCalculationResponse.class);
            builder.premium(MotorQuoteResponse.PremiumInfo.builder()
                    .basicPremium(premium.getBasicPremium())
                    .benefitsTotal(premium.getBenefitsTotal())
                    .netPremium(premium.getNetPremium())
                    .levies(premium.getLevies())
                    .stampDuty(premium.getStampDuty())
                    .grossPremium(premium.getGrossPremium())
                    .currency("KES")
                    .rateSetUsed(premium.getRateSetUsed())
                    .specialRateApplied(premium.isSpecialRateApplied())
                    .benefitsBreakdown(premium.getBenefitsBreakdown())
                    .grossPremiumBreakdown(premium.getGrossPremiumBreakdown())
                    .build());
        }

        if (app.getDraftQuoteResult() != null) {
            DraftQuoteResponse draft = deserialize(app.getDraftQuoteResult(), DraftQuoteResponse.class);
            builder.draftQuote(MotorQuoteResponse.DraftQuoteInfo.builder()
                    .draftQuoteSysId(draft.getDraftQuoteSysId())
                    .draftQuoteRef(draft.getDraftQuoteRef())
                    .status(draft.getStatus() != null ? draft.getStatus().name() : null)
                    .build());
        }

        if (app.getPaymentResult() != null) {
            try {
                // Try as Status response first
                MpesaPaymentStatusResponse status = objectMapper.readValue(app.getPaymentResult(), MpesaPaymentStatusResponse.class);
                if (status.getCheckoutId() != null && status.getStatus() != null) {
                    builder.payment(MotorQuoteResponse.PaymentInfo.builder()
                            .checkoutId(status.getCheckoutId())
                            .status(status.getStatus())
                            .message(status.getMessage())
                            .receiptNumber(status.getReceiptNumber())
                            .build());
                } else {
                    // Try as Initiation response
                    MpesaInitiatePaymentResponse init = objectMapper.readValue(app.getPaymentResult(), MpesaInitiatePaymentResponse.class);
                    builder.payment(MotorQuoteResponse.PaymentInfo.builder()
                            .checkoutId(init.getCheckoutId())
                            .status(init.getStatus())
                            .message(init.getMessage())
                            .build());
                }
            } catch (Exception ignored) {
            }
        }

        if (app.getPolicyIssuanceResult() != null) {
            PolicyIssuanceResult policy = deserialize(app.getPolicyIssuanceResult(), PolicyIssuanceResult.class);
            if (policy != null) {
                builder.policy(MotorQuoteResponse.PolicyInfo.builder()
                        .policyReference(policy.getPolicyReference())
                        .externalReference(policy.getExternalReference())
                        .emailSent(policy.isEmailSent())
                        .status(policy.getStatus())
                        .message(policy.getMessage())
                        .build());
            }
        }

        builder.nextActions(deriveNextActions(app.getStatus()));

        return builder.build();
    }

    private List<String> deriveNextActions(MotorQuoteStatus status) {
        List<String> actions = new ArrayList<>();
        switch (status) {
            case PREMIUM_CALCULATED:
                actions.add("ACCEPT_QUOTE");
                actions.add("RECALCULATE_PREMIUM");
                break;
            case QUOTE_ACCEPTED:
            case DRAFT_QUOTE_CREATED:
                actions.add("INITIATE_PAYMENT");
                break;
            case PAYMENT_INITIATED:
            case PAYMENT_PENDING:
                actions.add("CHECK_PAYMENT_STATUS");
                break;
            case PAYMENT_SUCCESSFUL:
                actions.add("ISSUE_POLICY");
                break;
            case PREMIUM_CALCULATION_FAILED:
            case PAYMENT_FAILED:
                actions.add("RETRY");
                break;
        }
        return actions;
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to {}", clazz.getSimpleName(), e);
            return null;
        }
    }
}
