package com.isec.platform.modules.integrations.sanlam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class SanlamMasterReferenceDataResponse {
    // Sanlam returns a Map where keys are strings (like "1" or "999") 
    // and values are Maps of category names to lists of items.
    private Map<String, Map<String, List<SanlamReferenceDataItem>>> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanlamReferenceDataItem {
        private String value;
        @JsonProperty("value_code")
        private String valueCode;
        @JsonProperty("str_attr_sys_id")
        private String strAttrSysId;
    }
}
