package com.isec.platform.modules.audit.repository;

import com.isec.platform.modules.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
