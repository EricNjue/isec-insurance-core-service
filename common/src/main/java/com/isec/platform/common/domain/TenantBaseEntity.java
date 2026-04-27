package com.isec.platform.common.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class TenantBaseEntity extends BaseEntity {

    private String tenantId;
}
