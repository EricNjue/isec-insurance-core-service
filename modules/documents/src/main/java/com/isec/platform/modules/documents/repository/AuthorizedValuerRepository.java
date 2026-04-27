package com.isec.platform.modules.documents.repository;

import com.isec.platform.modules.documents.domain.AuthorizedValuer;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface AuthorizedValuerRepository extends ReactiveCrudRepository<AuthorizedValuer, Long> {
    Flux<AuthorizedValuer> findByActiveTrue();
}
