package com.isec.platform.modules.rating.dto;

import com.isec.platform.modules.rating.domain.RuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateBookDto implements Serializable {
    private Long id;
    private String tenantId;
    private String name;
    private String versionName;
    private List<RateRuleDto> rules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateRuleDto implements Serializable {
        private Long id;
        private RuleType ruleType;
        private String category;
        private String description;
        private int priority;
        private String conditionExpression;
        private String valueExpression;
    }
}
