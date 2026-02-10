package com.isec.platform.modules.rating.dto;

import com.isec.platform.modules.rating.domain.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RateRuleRequest {
    @NotNull
    private Long rateBookId;
    
    @NotNull
    private RuleType ruleType;
    
    @NotBlank
    private String category;
    
    @NotBlank
    private String description;
    
    private int priority;
    
    private String conditionExpression;
    
    @NotBlank
    private String valueExpression;
}
