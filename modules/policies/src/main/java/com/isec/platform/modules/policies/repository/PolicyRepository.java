package com.isec.platform.modules.policies.repository;

import com.isec.platform.modules.policies.domain.Policy;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface PolicyRepository extends ReactiveCrudRepository<Policy, Long> {
    Mono<Policy> findByApplicationId(Long applicationId);
    Mono<Policy> findByPolicyNumber(String policyNumber);
}
