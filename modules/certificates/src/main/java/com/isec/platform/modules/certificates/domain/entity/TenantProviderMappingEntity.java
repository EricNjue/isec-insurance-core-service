package com.isec.platform.modules.certificates.domain.entity;

import com.isec.platform.common.domain.TenantBaseEntity;
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
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(
        name = "tenant_provider_mapping",
        indexes = {
                @Index(name = "idx_tenant_provider_tenant", columnList = "tenant_id"),
                @Index(name = "idx_tenant_provider_provider", columnList = "provider_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantProviderMappingEntity extends TenantBaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_code", nullable = false)
    private ProviderType providerCode;

    @Column(name = "active", nullable = false)
    private Boolean active;
}
