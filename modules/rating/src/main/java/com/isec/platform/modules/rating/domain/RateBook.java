package com.isec.platform.modules.rating.domain;

import com.isec.platform.common.domain.TenantBaseEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;

import java.time.LocalDateTime;

@Table("rate_books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateBook extends TenantBaseEntity {

    @Id
    private Long id;

    private String name;

    private String versionName;

    private boolean active;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;
}
