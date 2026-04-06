package com.isec.platform.modules.integrations.common.dto.referencedata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.isec.platform.modules.integrations.common.enums.ReferenceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MasterReferenceDataResponse {
    private String companyCode;
    private String productCode;
    private Map<ReferenceCategory, List<ReferenceDataItem>> categories;
    private boolean servedFromCache;
    private OffsetDateTime lastRefreshedAt;
}
