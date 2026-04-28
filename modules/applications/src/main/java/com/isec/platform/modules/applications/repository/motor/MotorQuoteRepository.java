package com.isec.platform.modules.applications.repository.motor;

import com.isec.platform.modules.applications.domain.motor.MotorQuoteApplication;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface MotorQuoteRepository extends ReactiveCrudRepository<MotorQuoteApplication, Long> {
    Mono<MotorQuoteApplication> findByQuoteId(String quoteId);
}
