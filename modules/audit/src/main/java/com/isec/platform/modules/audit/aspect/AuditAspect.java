package com.isec.platform.modules.audit.aspect;

import com.isec.platform.modules.audit.domain.AuditLog;
import com.isec.platform.modules.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @AfterReturning(pointcut = "execution(* com.isec.platform.modules..service.*.save*(..)) || execution(* com.isec.platform.modules..service.*.update*(..))", returning = "result")
    public void logAction(JoinPoint joinPoint, Object result) {
        String actor = SecurityContextHolder.getContext().getAuthentication() != null ? 
                SecurityContextHolder.getContext().getAuthentication().getName() : "SYSTEM";
        
        AuditLog log = AuditLog.builder()
                .actor(actor)
                .action(joinPoint.getSignature().getName())
                .entityType(result != null ? result.getClass().getSimpleName() : "UNKNOWN")
                .detail("Method executed: " + joinPoint.getSignature().toShortString())
                .build();
        
        auditLogRepository.save(log);
    }
}
