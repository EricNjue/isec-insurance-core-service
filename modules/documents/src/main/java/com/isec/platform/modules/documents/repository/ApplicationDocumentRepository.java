package com.isec.platform.modules.documents.repository;

import com.isec.platform.modules.documents.domain.ApplicationDocument;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ApplicationDocumentRepository extends ReactiveCrudRepository<ApplicationDocument, Long> {
    Flux<ApplicationDocument> findByApplicationId(Long applicationId);
    Mono<ApplicationDocument> findByApplicationIdAndDocumentType(Long applicationId, String documentType);
}
