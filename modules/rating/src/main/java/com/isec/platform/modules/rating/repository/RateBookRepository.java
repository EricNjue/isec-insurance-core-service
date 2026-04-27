package com.isec.platform.modules.rating.repository;

import com.isec.platform.modules.rating.domain.RateBook;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface RateBookRepository extends ReactiveCrudRepository<RateBook, Long> {

    Mono<RateBook> findByTenantIdAndActiveTrue(String tenantId);

    Flux<RateBook> findAllByTenantId(String tenantId);
}
