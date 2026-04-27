package com.isec.platform.modules.audit.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDateTime;

@Table("audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    private Long id;

    private String actor;
    private String action;
    private String entityType;
    private Long entityId;
    
    private String detail;
    
    private LocalDateTime timestamp;
}
