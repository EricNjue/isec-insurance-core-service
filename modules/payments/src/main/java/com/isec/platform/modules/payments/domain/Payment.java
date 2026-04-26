package com.isec.platform.modules.payments.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    private Long id;

    private Long applicationId;

    private BigDecimal amount;

    private String mpesaReceiptNumber;

    private String status; // PENDING, COMPLETED, FAILED

    private String phoneNumber;

    private String checkoutRequestId;

    private LocalDateTime createdAt;
}
