package com.isec.platform.modules.payments.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payments")
public class Payment {
    @Id
    private UUID id;
    private UUID policyId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}
