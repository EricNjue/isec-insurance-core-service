package com.isec.platform.modules.integrations.mpesa.repository;

import com.isec.platform.modules.integrations.mpesa.domain.MpesaRequestLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface MpesaRequestLogRepository extends ReactiveCrudRepository<MpesaRequestLog, Long> {
    Mono<MpesaRequestLog> findByCheckoutRequestId(String checkoutRequestId);
}
