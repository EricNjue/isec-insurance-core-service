package com.isec.platform.common.multitenancy;

import lombok.extern.slf4j.Slf4j;

import reactor.util.context.Context;
import reactor.util.context.ContextView;
import reactor.core.publisher.Mono;

/**
 * Holder for the current tenant ID using Reactor Context.
 */
@Slf4j
public class TenantContext {

    private static final String TENANT_KEY = "tenantId";

    public static Context withTenantId(String tenantId) {
        return Context.of(TENANT_KEY, tenantId);
    }

    public static Mono<String> getTenantId() {
        return Mono.deferContextual(contextView -> 
            Mono.justOrEmpty(contextView.getOrEmpty(TENANT_KEY).map(Object::toString))
        );
    }
}
