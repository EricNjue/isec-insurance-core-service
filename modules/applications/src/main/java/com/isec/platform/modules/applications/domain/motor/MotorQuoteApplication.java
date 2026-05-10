package com.isec.platform.modules.applications.domain.motor;

import com.isec.platform.common.domain.TenantBaseEntity;
import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("motor_quote_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotorQuoteApplication extends TenantBaseEntity {
    @Id
    private Long id;
    private String quoteId;
    private PartnerType partner;
    private MotorQuoteStatus status;
    
    // JSON details
    private String insuranceDetails;
    private String vehicleDetails;
    private String kycDetails;
    
    // Results
    private String premiumResult;
    private String draftQuoteResult;
    private String paymentResult;
    private String policyIssuanceResult;
    private String dmvicCheckResult;
    
    // Partner references
    private String partnerReferences;
    private String rawPartnerResponses;
}
