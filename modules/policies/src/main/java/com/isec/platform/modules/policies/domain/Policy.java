package com.isec.platform.modules.policies.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Table("policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {
    @Id
    private Long id;

    private Long applicationId;

    private String policyNumber;

    private LocalDate startDate;

    private LocalDate expiryDate;

    private BigDecimal totalAnnualPremium;

    private BigDecimal balance;

    private Boolean isActive;

    private String valuationLetterS3Key;
}
