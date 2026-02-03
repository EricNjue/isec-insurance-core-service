package com.isec.platform.modules.payments.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(unique = true)
    private String mpesaReceiptNumber;

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, FAILED

    private String phoneNumber;

    private String checkoutRequestId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
