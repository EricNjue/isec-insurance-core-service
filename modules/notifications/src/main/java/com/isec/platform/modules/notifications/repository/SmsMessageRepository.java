package com.isec.platform.modules.notifications.repository;

import com.isec.platform.modules.notifications.model.SmsMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SmsMessageRepository extends JpaRepository<SmsMessage, UUID> {
}
