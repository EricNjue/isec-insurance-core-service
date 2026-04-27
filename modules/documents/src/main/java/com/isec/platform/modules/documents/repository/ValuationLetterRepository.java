package com.isec.platform.modules.documents.repository;

import com.isec.platform.modules.documents.domain.ValuationLetter;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ValuationLetterRepository extends ReactiveCrudRepository<ValuationLetter, Long> {
    Flux<ValuationLetter> findByPolicyId(Long policyId);
    Mono<ValuationLetter> findFirstByPolicyIdAndGeneratedAtAfter(Long policyId, LocalDateTime generatedAt);
    Mono<ValuationLetter> findFirstByPolicyIdOrderByGeneratedAtDesc(Long policyId);
    Mono<ValuationLetter> findFirstByPolicyNumberOrderByGeneratedAtDesc(String policyNumber);
    Mono<ValuationLetter> findByDocumentUuid(UUID documentUuid);
}
