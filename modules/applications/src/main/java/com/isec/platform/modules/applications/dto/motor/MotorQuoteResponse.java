package com.isec.platform.modules.applications.dto.motor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.isec.platform.modules.applications.domain.motor.MotorQuoteStatus;
import com.isec.platform.modules.applications.dto.QuoteRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatus;
import com.isec.platform.modules.integrations.premium.model.PremiumBenefitBreakdown;
import com.isec.platform.modules.integrations.premium.model.PremiumGrossBreakdown;
import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MotorQuoteResponse {
    private String quoteId;
    private PartnerType partner;
    private MotorQuoteStatus status;
    private QuoteRequest.InsuranceDetails insuranceDetails;
    private QuoteRequest.VehicleDetails vehicleDetails;
    private QuoteRequest.KycDetails kycDetails;
    private PremiumInfo premium;
    private DraftQuoteInfo draftQuote;
    private PaymentInfo payment;
    private List<String> nextActions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PremiumInfo {
        private BigDecimal basicPremium;
        private BigDecimal benefitsTotal;
        private BigDecimal netPremium;
        private BigDecimal levies;
        private BigDecimal stampDuty;
        private BigDecimal grossPremium;
        private String currency;
        private String rateSetUsed;
        private boolean specialRateApplied;
        private List<PremiumBenefitBreakdown> benefitsBreakdown;
        private List<PremiumGrossBreakdown> grossPremiumBreakdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DraftQuoteInfo {
        private Long draftQuoteSysId;
        private String draftQuoteRef;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private String checkoutId;
        private MpesaPaymentStatus status;
        private String receiptNumber;
        private String message;
    }
}
