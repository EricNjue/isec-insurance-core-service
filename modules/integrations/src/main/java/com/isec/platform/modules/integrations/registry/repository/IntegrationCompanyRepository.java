package com.isec.platform.modules.integrations.registry.repository;

import com.isec.platform.modules.integrations.registry.entity.IntegrationCompany;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface IntegrationCompanyRepository extends ReactiveCrudRepository<IntegrationCompany, Long> {
    Mono<IntegrationCompany> findByCode(String code);
    Mono<Boolean> existsByCode(String code);
    Flux<IntegrationCompany> findAllByActiveTrue();
    Flux<IntegrationCompany> findAll();
}
