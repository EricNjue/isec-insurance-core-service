package com.isec.platform.modules.integrations.common.dto.referencedata;

import com.isec.platform.modules.integrations.common.enums.ReferenceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependentReferenceDataResponse {
    private String companyCode;
    private String productCode;
    private ReferenceCategory parentCategoryKey;
    private String parentValue;
    private ReferenceCategory childCategoryKey;
    private List<ReferenceDataItem> items;
    private boolean servedFromCache;
    private OffsetDateTime lastRefreshedAt;
}
