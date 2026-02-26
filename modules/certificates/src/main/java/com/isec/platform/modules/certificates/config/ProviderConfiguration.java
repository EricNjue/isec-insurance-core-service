package com.isec.platform.modules.certificates.config;

import com.isec.platform.modules.certificates.domain.canonical.ProviderType;

public record ProviderConfiguration(
        ProviderType providerType,
        String baseUrl,
        String authType,
        int timeoutMs,
        int retryCount,
        boolean active
) {
}
