package com.isec.platform.modules.integrations.sanlam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanlamDependentReferenceDataResponse {
    // Sanlam returns a Map where keys are child category names (e.g. "Vehicle Model")
    private Map<String, List<SanlamMasterReferenceDataResponse.SanlamReferenceDataItem>> data;
}
