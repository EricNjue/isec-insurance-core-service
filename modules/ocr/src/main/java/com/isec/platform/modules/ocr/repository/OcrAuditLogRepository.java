package com.isec.platform.modules.ocr.repository;

import com.isec.platform.modules.ocr.domain.OcrAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OcrAuditLogRepository extends JpaRepository<OcrAuditLog, UUID> {
}
