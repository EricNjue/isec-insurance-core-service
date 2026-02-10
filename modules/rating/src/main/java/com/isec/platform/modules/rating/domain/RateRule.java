package com.isec.platform.modules.rating.domain;

import com.isec.platform.common.domain.TenantBaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rate_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateRule extends TenantBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_book_id", nullable = false)
    private RateBook rateBook;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    @Column(nullable = false)
    private String category; // e.g., "PRIVATE_CAR", "COMMERCIAL"

    @Column(length = 500)
    private String description;

    private int priority;

    @Column(name = "condition_expression", columnDefinition = "TEXT")
    private String conditionExpression; // SpEL expression

    @Column(name = "value_expression", columnDefinition = "TEXT")
    private String valueExpression; // SpEL expression or constant
}
