package com.isec.platform.modules.applications.repository;

import com.isec.platform.modules.applications.domain.Application;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ApplicationRepository extends ReactiveCrudRepository<Application, Long> {
    Flux<Application> findByUserIdAndTenantId(String userId, String tenantId);
    Flux<Application> findAllByTenantId(String tenantId);
}
