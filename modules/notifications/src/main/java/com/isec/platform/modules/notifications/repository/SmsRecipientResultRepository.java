package com.isec.platform.modules.notifications.repository;

import com.isec.platform.modules.notifications.model.SmsRecipientResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SmsRecipientResultRepository extends JpaRepository<SmsRecipientResult, UUID> {
    Optional<SmsRecipientResult> findByMessageId(String messageId);
}
