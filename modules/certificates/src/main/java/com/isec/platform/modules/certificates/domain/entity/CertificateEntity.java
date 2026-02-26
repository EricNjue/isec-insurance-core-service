package com.isec.platform.modules.certificates.domain.entity;

import com.isec.platform.common.domain.TenantBaseEntity;
import com.isec.platform.modules.certificates.domain.canonical.CertificateStatus;
import com.isec.platform.modules.certificates.domain.canonical.CertificateType;
import com.isec.platform.modules.certificates.domain.canonical.ProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "certificate",
        indexes = {
                @Index(name = "idx_certificate_idempotency", columnList = "idempotency_key", unique = true),
                @Index(name = "idx_certificate_provider_code", columnList = "provider_code"),
                @Index(name = "idx_certificate_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_certificate_external_reference", columnList = "external_reference")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateEntity extends TenantBaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_code", nullable = false)
    private ProviderType providerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type")
    private CertificateType certificateType;

    @Column(name = "certificate_number")
    private String certificateNumber;

    @Column(name = "external_reference")
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateStatus status;

    @Column(name = "premium_amount", precision = 19, scale = 2)
    private BigDecimal premiumAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", columnDefinition = "jsonb")
    private String requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    @Column(name = "issued_at")
    private Instant issuedAt;
}
