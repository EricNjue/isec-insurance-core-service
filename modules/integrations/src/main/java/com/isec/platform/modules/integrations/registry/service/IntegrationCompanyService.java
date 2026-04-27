package com.isec.platform.modules.integrations.registry.service;

import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.modules.integrations.registry.dto.*;
import com.isec.platform.modules.integrations.registry.entity.IntegrationCompany;
import com.isec.platform.modules.integrations.registry.mapper.IntegrationCompanyMapper;
import com.isec.platform.modules.integrations.registry.repository.IntegrationCompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationCompanyService {

    private final IntegrationCompanyRepository repository;
    private final IntegrationCompanyMapper mapper;

    public Flux<IntegrationCompanyPublicResponse> getPublicIntegrations(Boolean activeOnly) {
        Flux<IntegrationCompany> companies;
        if (activeOnly == null || activeOnly) {
            companies = repository.findAllByActiveTrue();
        } else {
            // R2DBC repository does not have findAll() by default in some versions, 
            // but we added it to our interface
            companies = repository.findAll();
        }
        return companies.map(mapper::toPublicResponse);
    }

    public Flux<IntegrationCompanyResponse> getAllIntegrations() {
        return repository.findAll().map(mapper::toResponse);
    }

    public Mono<IntegrationCompanyResponse> getIntegrationById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .switchIfEmpty(Mono.error(new BusinessException("Integration not found with ID: " + id)));
    }

    public Mono<IntegrationCompanyResponse> createIntegration(IntegrationCompanyRequest request) {
        String normalizedCode = request.getCode().toUpperCase().trim();
        return repository.existsByCode(normalizedCode)
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new BusinessException("INTEGRATION_ALREADY_EXISTS: Integration with code " + normalizedCode + " already exists"));
                }

                IntegrationCompany entity = mapper.toEntity(request);
                entity.setCode(normalizedCode);
                entity.setCreatedBy("SYSTEM_ADMIN"); 

                return repository.save(entity)
                    .doOnNext(saved -> log.info("Created new integration company: {} ({})", saved.getName(), saved.getCode()))
                    .map(mapper::toResponse);
            });
    }

    public Mono<IntegrationCompanyResponse> updateIntegration(Long id, IntegrationCompanyRequest request) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("INTEGRATION_NOT_FOUND: Integration not found with ID: " + id)))
                .flatMap(entity -> {
                    // Code change is generally not allowed
                    if (!entity.getCode().equalsIgnoreCase(request.getCode())) {
                        log.warn("Attempted to change immutable code for integration ID {}. Original: {}, Requested: {}", 
                                id, entity.getCode(), request.getCode());
                    }

                    mapper.updateEntity(entity, request);
                    entity.setUpdatedBy("SYSTEM_ADMIN");

                    return repository.save(entity)
                        .doOnNext(updated -> log.info("Updated integration company: {} ({})", updated.getName(), updated.getCode()))
                        .map(mapper::toResponse);
                });
    }

    public Mono<Void> updateStatus(Long id, IntegrationStatusRequest request) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("INTEGRATION_NOT_FOUND: Integration not found with ID: " + id)))
                .flatMap(entity -> {
                    entity.setActive(request.getActive());
                    entity.setUpdatedBy("SYSTEM_ADMIN");
                    return repository.save(entity)
                        .doOnNext(saved -> log.info("Updated status for integration {}: active={}", entity.getCode(), request.getActive()))
                        .then();
                });
    }

    public Mono<Void> deleteIntegration(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("INTEGRATION_NOT_FOUND: Integration not found with ID: " + id)))
                .flatMap(entity -> {
                    entity.setDeleted(true);
                    entity.setActive(false);
                    entity.setUpdatedBy("SYSTEM_ADMIN");
                    return repository.save(entity)
                        .doOnNext(saved -> log.info("Soft deleted integration company: {} ({})", entity.getName(), entity.getCode()))
                        .then();
                });
    }
}
