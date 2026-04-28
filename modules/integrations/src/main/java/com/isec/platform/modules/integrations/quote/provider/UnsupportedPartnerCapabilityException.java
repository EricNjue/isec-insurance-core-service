package com.isec.platform.modules.integrations.quote.provider;

import lombok.Getter;

@Getter
public class UnsupportedPartnerCapabilityException extends RuntimeException {
    private final PartnerType partnerType;
    private final QuoteLifecycleCapability capability;

    public UnsupportedPartnerCapabilityException(PartnerType partnerType, QuoteLifecycleCapability capability) {
        super(String.format("Partner %s does not support capability %s", partnerType, capability));
        this.partnerType = partnerType;
        this.capability = capability;
    }
}
