package com.isec.platform.modules.integrations.registry.entity;

import com.isec.platform.common.domain.BaseEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;

@Table("integration_companies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationCompany extends BaseEntity {

    @Id
    private Long id;

    private String code;

    private String name;

    private String description;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean deleted = false;

    private String createdBy;

    private String updatedBy;
}
