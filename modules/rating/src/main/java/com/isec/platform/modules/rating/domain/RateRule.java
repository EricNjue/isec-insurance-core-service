package com.isec.platform.modules.rating.domain;

import com.isec.platform.common.domain.TenantBaseEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;

@Table("rate_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateRule extends TenantBaseEntity {

    @Id
    private Long id;

    private Long rateBookId;

    private RuleType ruleType;

    private String category; // e.g., "PRIVATE_CAR", "COMMERCIAL"

    private String description;

    private int priority;

    private String conditionExpression; // SpEL expression

    private String valueExpression; // SpEL expression or constant
}
