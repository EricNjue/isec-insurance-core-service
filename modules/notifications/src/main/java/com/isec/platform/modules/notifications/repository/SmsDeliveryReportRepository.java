package com.isec.platform.modules.notifications.repository;

import com.isec.platform.modules.notifications.model.SmsDeliveryReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SmsDeliveryReportRepository extends JpaRepository<SmsDeliveryReport, UUID> {
    Optional<SmsDeliveryReport> findByMessageId(String messageId);
}
