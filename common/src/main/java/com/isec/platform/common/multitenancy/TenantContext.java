package com.isec.platform.common.multitenancy;

import lombok.extern.slf4j.Slf4j;

/**
 * Holder for the current tenant ID.
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        log.trace("Setting tenantId to: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        log.trace("Clearing tenantId");
        CURRENT_TENANT.remove();
    }
}
