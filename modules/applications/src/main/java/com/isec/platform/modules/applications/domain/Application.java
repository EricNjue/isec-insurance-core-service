package com.isec.platform.modules.applications.domain;

import com.isec.platform.common.domain.TenantBaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application extends TenantBaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String registrationNumber;

    @Column(nullable = false)
    private String vehicleMake;

    @Column(nullable = false)
    private String vehicleModel;

    @Column(nullable = false)
    private Integer yearOfManufacture;

    @Column(nullable = false)
    private BigDecimal vehicleValue;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Column(name = "quote_id")
    private String quoteId;

    @Column(name = "rate_book_id")
    private Long rateBookId;

    @Column(name = "pricing_snapshot", columnDefinition = "TEXT")
    private String pricingSnapshot;

    @Column(name = "referral_reason")
    private String referralReason;

    @Column(name = "underwriter_comments", columnDefinition = "TEXT")
    private String underwriter_comments;

    @Column(name = "underwriter_id")
    private String underwriterId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
