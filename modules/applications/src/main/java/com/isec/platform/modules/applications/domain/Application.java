package com.isec.platform.modules.applications.domain;

import com.isec.platform.common.domain.TenantBaseEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application extends TenantBaseEntity {
    @Id
    private Long id;

    private String userId;

    private String registrationNumber;

    private String vehicleMake;

    private String vehicleModel;

    private Integer yearOfManufacture;

    private BigDecimal vehicleValue;

    private String chassisNumber;

    private String engineNumber;

    private ApplicationStatus status;

    private String quoteId;

    private Long rateBookId;

    private String pricingSnapshot;

    private String referralReason;

    private String underwriter_comments;

    private String underwriterId;

    private LocalDateTime approvedAt;
}
