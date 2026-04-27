package com.isec.platform.modules.rating.repository;

import com.isec.platform.modules.rating.domain.RateRule;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface RateRuleRepository extends ReactiveCrudRepository<RateRule, Long> {
    Flux<RateRule> findAllByRateBookId(Long rateBookId);
}
