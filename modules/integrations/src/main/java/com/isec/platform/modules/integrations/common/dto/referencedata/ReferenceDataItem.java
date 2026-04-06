package com.isec.platform.modules.integrations.common.dto.referencedata;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ReferenceDataItem {
    private String code;
    private String label;
    private String sourceId;
    private Map<String, Object> metadata;
}
