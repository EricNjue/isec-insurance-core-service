package com.isec.platform.modules.audit.aspect;

import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.audit.domain.AuditLog;
import com.isec.platform.modules.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @AfterReturning(pointcut = "execution(* com.isec.platform.modules..service.*.save*(..)) || execution(* com.isec.platform.modules..service.*.update*(..))", returning = "result")
    public void logAction(JoinPoint joinPoint, Object result) {
        if (!(result instanceof Mono || result instanceof reactor.core.publisher.Flux)) {
            // If it's not reactive, we might be in a bit of trouble or it's a synchronous method we still want to audit
            // But in a WebFlux app, services should return Mono/Flux.
            return;
        }

        if (result instanceof Mono) {
            ((Mono<?>) result).flatMap(res -> createAuditLog(joinPoint, res)).subscribe();
        } else if (result instanceof reactor.core.publisher.Flux) {
            ((reactor.core.publisher.Flux<?>) result).collectList()
                    .flatMap(list -> createAuditLog(joinPoint, list.isEmpty() ? null : list.get(0)))
                    .subscribe();
        }
    }

    private Mono<Void> createAuditLog(JoinPoint joinPoint, Object res) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> auth != null ? auth.getName() : "SYSTEM")
                .defaultIfEmpty("SYSTEM")
                .zipWith(TenantContext.getTenantId().defaultIfEmpty("DEFAULT"))
                .flatMap(tuple -> {
                    String actor = tuple.getT1();
                    String tenantId = tuple.getT2();

                    log.debug("Auditing action: {} by actor: {} for tenant: {}", joinPoint.getSignature().getName(), actor, tenantId);

                    AuditLog auditLog = AuditLog.builder()
                            .actor(actor)
                            .action(joinPoint.getSignature().getName())
                            .entityType(res != null ? res.getClass().getSimpleName() : "UNKNOWN")
                            .detail("Method executed: " + joinPoint.getSignature().toShortString())
                            // assuming AuditLog has a tenantId field if we want to use it
                            .build();

                    return auditLogRepository.save(auditLog)
                            .doOnNext(saved -> log.debug("Audit log saved with ID: {}", saved.getId()))
                            .then();
                });
    }
}
