package com.isec.platform.modules.certificates.domain.entity;

import com.isec.platform.common.domain.BaseEntity;
import com.isec.platform.modules.certificates.domain.canonical.ProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "insurance_provider")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceProviderEntity extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_code", nullable = false, unique = true)
    private ProviderType providerCode;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "auth_type")
    private String authType;

    @Column(name = "timeout_ms")
    private Integer timeoutMs;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "active", nullable = false)
    private Boolean active;
}
