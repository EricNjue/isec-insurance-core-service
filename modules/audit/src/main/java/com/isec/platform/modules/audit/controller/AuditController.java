package com.isec.platform.modules.audit.controller;

import com.isec.platform.modules.audit.domain.AuditLog;
import com.isec.platform.modules.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll()
                .skip(pageable.getOffset())
                .take(pageable.getPageSize());
    }

    @GetMapping("/entity/{type}/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<AuditLog> getAuditLogsByEntity(
            @PathVariable String type,
            @PathVariable Long id,
            Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(type, id, pageable);
    }
}
