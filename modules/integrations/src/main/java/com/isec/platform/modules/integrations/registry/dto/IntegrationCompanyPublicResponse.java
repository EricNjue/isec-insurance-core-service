package com.isec.platform.modules.integrations.registry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationCompanyPublicResponse {
    private String code;
    private String name;
    private boolean active;
}
