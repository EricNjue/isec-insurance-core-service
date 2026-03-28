package com.isec.platform.modules.integrations.registry.mapper;

import com.isec.platform.modules.integrations.registry.dto.IntegrationCompanyPublicResponse;
import com.isec.platform.modules.integrations.registry.dto.IntegrationCompanyRequest;
import com.isec.platform.modules.integrations.registry.dto.IntegrationCompanyResponse;
import com.isec.platform.modules.integrations.registry.entity.IntegrationCompany;
import org.springframework.stereotype.Component;

@Component
public class IntegrationCompanyMapper {

    public IntegrationCompany toEntity(IntegrationCompanyRequest request) {
        if (request == null) return null;
        return IntegrationCompany.builder()
                .code(request.getCode().toUpperCase().trim())
                .name(request.getName())
                .description(request.getDescription())
                .active(request.isActive())
                .build();
    }

    public IntegrationCompanyResponse toResponse(IntegrationCompany entity) {
        if (entity == null) return null;
        return IntegrationCompanyResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public IntegrationCompanyPublicResponse toPublicResponse(IntegrationCompany entity) {
        if (entity == null) return null;
        return IntegrationCompanyPublicResponse.builder()
                .code(entity.getCode())
                .name(entity.getName())
                .active(entity.isActive())
                .build();
    }

    public void updateEntity(IntegrationCompany entity, IntegrationCompanyRequest request) {
        if (request == null || entity == null) return;
        // code is immutable as per requirement or best practice
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setActive(request.isActive());
    }
}
