package com.isec.platform.modules.notifications.repository;

import com.isec.platform.modules.notifications.model.SmsMessage;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import java.util.UUID;

public interface SmsMessageRepository extends ReactiveCrudRepository<SmsMessage, UUID> {
}
