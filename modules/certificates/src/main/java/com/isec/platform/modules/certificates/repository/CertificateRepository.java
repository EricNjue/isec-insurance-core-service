package com.isec.platform.modules.certificates.repository;

import com.isec.platform.modules.certificates.domain.Certificate;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CertificateRepository extends ReactiveCrudRepository<Certificate, Long> {
    Flux<Certificate> findByPolicyId(Long policyId);
    Mono<Certificate> findByPolicyNumber(String policyNumber);
    Mono<Certificate> findByPartnerCodeAndCertificateNumber(String partnerCode, String certificateNumber);
    Mono<Certificate> findByIdempotencyKey(String idempotencyKey);
}
