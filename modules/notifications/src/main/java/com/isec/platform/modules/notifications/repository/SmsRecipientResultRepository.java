package com.isec.platform.modules.notifications.repository;

import com.isec.platform.modules.notifications.model.SmsRecipientResult;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface SmsRecipientResultRepository extends ReactiveCrudRepository<SmsRecipientResult, UUID> {
    Mono<SmsRecipientResult> findByMessageId(String messageId);
}
