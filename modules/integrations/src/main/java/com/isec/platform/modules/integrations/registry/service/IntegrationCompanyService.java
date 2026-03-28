package com.isec.platform.modules.integrations.registry.service;

import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.modules.integrations.registry.dto.*;
import com.isec.platform.modules.integrations.registry.entity.IntegrationCompany;
import com.isec.platform.modules.integrations.registry.mapper.IntegrationCompanyMapper;
import com.isec.platform.modules.integrations.registry.repository.IntegrationCompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationCompanyService {

    private final IntegrationCompanyRepository repository;
    private final IntegrationCompanyMapper mapper;

    @Transactional(readOnly = true)
    public List<IntegrationCompanyPublicResponse> getPublicIntegrations(Boolean activeOnly) {
        List<IntegrationCompany> companies;
        if (activeOnly == null || activeOnly) {
            companies = repository.findAllByActiveTrue();
        } else {
            companies = repository.findAll();
        }
        return companies.stream()
                .map(mapper::toPublicResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IntegrationCompanyResponse> getAllIntegrations() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IntegrationCompanyResponse getIntegrationById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new BusinessException("Integration not found with ID: " + id));
    }

    @Transactional
    public IntegrationCompanyResponse createIntegration(IntegrationCompanyRequest request) {
        String normalizedCode = request.getCode().toUpperCase().trim();
        if (repository.existsByCode(normalizedCode)) {
            throw new BusinessException("INTEGRATION_ALREADY_EXISTS: Integration with code " + normalizedCode + " already exists");
        }

        IntegrationCompany entity = mapper.toEntity(request);
        // Ensure normalization
        entity.setCode(normalizedCode);
        
        // In a real app, we'd get the current user from SecurityContext
        entity.setCreatedBy("SYSTEM_ADMIN"); 

        IntegrationCompany saved = repository.save(entity);
        log.info("Created new integration company: {} ({})", saved.getName(), saved.getCode());
        return mapper.toResponse(saved);
    }

    @Transactional
    public IntegrationCompanyResponse updateIntegration(Long id, IntegrationCompanyRequest request) {
        IntegrationCompany entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException("INTEGRATION_NOT_FOUND: Integration not found with ID: " + id));

        // Code change is generally not allowed for integrations as it may be used as a key in other systems
        if (!entity.getCode().equalsIgnoreCase(request.getCode())) {
            log.warn("Attempted to change immutable code for integration ID {}. Original: {}, Requested: {}", 
                    id, entity.getCode(), request.getCode());
            // We can either ignore it or throw an error. Choosing to keep it immutable and ignore the change in mapper.
        }

        mapper.updateEntity(entity, request);
        entity.setUpdatedBy("SYSTEM_ADMIN");

        IntegrationCompany updated = repository.save(entity);
        log.info("Updated integration company: {} ({})", updated.getName(), updated.getCode());
        return mapper.toResponse(updated);
    }

    @Transactional
    public void updateStatus(Long id, IntegrationStatusRequest request) {
        IntegrationCompany entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException("INTEGRATION_NOT_FOUND: Integration not found with ID: " + id));
        
        entity.setActive(request.getActive());
        entity.setUpdatedBy("SYSTEM_ADMIN");
        repository.save(entity);
        log.info("Updated status for integration {}: active={}", entity.getCode(), request.getActive());
    }

    @Transactional
    public void deleteIntegration(Long id) {
        IntegrationCompany entity = repository.findById(id)
                .orElseThrow(() -> new BusinessException("INTEGRATION_NOT_FOUND: Integration not found with ID: " + id));

        // Soft delete
        entity.setDeleted(true);
        entity.setActive(false);
        entity.setUpdatedBy("SYSTEM_ADMIN");
        repository.save(entity);
        log.info("Soft deleted integration company: {} ({})", entity.getName(), entity.getCode());
    }
}
