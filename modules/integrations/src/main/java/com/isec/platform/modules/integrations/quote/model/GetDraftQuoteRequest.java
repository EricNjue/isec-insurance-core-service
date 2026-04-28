package com.isec.platform.modules.integrations.quote.model;

import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetDraftQuoteRequest {
    private PartnerType provider;
    private Long draftQuoteSysId;
    private String draftQuoteRef;
    private Map<String, Object> metadata;
}
