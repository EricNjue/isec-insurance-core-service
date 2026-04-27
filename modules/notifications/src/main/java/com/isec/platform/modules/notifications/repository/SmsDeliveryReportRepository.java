package com.isec.platform.modules.notifications.repository;

import com.isec.platform.modules.notifications.model.SmsDeliveryReport;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface SmsDeliveryReportRepository extends ReactiveCrudRepository<SmsDeliveryReport, UUID> {
    Mono<SmsDeliveryReport> findByMessageId(String messageId);
}
