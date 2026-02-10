package com.isec.platform.common.domain;

import com.isec.platform.common.multitenancy.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class TenantBaseEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @PrePersist
    @Override
    public void onCreate() {
        super.onCreate();
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getTenantId();
        }
        if (this.tenantId == null) {
             throw new IllegalStateException("Tenant ID must be set in TenantContext for " + this.getClass().getSimpleName());
        }
    }
}
