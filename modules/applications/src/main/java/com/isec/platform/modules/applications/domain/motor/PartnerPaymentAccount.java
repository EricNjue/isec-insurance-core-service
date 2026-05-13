package com.isec.platform.modules.applications.domain.motor;

import com.isec.platform.common.domain.TenantBaseEntity;
import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("partner_payment_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerPaymentAccount extends TenantBaseEntity {
    @Id
    @Column("id")
    private UUID id;

    @Column("partner_code")
    private PartnerType partnerCode;

    @Column("payment_provider")
    private String paymentProvider; // e.g., MPESA, AIRTEL, CARD

    @Column("payment_method")
    private PaymentMethod paymentMethod;

    @Column("account_type")
    private String accountType; // e.g., PAYBILL, BUY_GOODS

    @Column("business_number")
    private String businessNumber;

    @Column("account_name")
    private String accountName;

    @Column("currency")
    private String currency;

    @Column("environment")
    private String environment;

    @Column("is_default")
    private boolean isDefault;

    @Column("is_active")
    private boolean isActive;

    @Column("metadata")
    private String metadata; // JSON string
}
