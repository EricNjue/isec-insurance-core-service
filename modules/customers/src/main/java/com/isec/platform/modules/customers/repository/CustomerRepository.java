package com.isec.platform.modules.customers.repository;

import com.isec.platform.modules.customers.domain.Customer;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CustomerRepository extends ReactiveCrudRepository<Customer, Long> {
    Mono<Customer> findByUserId(String userId);
}
