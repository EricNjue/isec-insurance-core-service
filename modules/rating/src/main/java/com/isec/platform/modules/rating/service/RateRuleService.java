package com.isec.platform.modules.rating.service;

import com.isec.platform.common.exception.ResourceNotFoundException;
import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.common.security.SecurityContextService;
import com.isec.platform.modules.rating.domain.RateBook;
import com.isec.platform.modules.rating.domain.RateRule;
import com.isec.platform.modules.rating.dto.RateRuleRequest;
import com.isec.platform.modules.rating.repository.RateBookRepository;
import com.isec.platform.modules.rating.repository.RateRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateRuleService {

    private final RateRuleRepository rateRuleRepository;
    private final RateBookRepository rateBookRepository;
    private final SecurityContextService securityContextService;
    private final RateBookSnapshotLoader snapshotLoader;

    public Mono<RateRule> createRule(RateRuleRequest request) {
        return rateBookRepository.findById(request.getRateBookId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("RateBook", request.getRateBookId())))
                .flatMap(rateBook -> validateTenantAccess(rateBook.getTenantId()).thenReturn(rateBook))
                .flatMap(rateBook -> {
                    RateRule rule = RateRule.builder()
                            .rateBookId(rateBook.getId())
                            .ruleType(request.getRuleType())
                            .category(request.getCategory())
                            .description(request.getDescription())
                            .priority(request.getPriority())
                            .conditionExpression(request.getConditionExpression())
                            .valueExpression(request.getValueExpression())
                            .build();
                    rule.setTenantId(rateBook.getTenantId());

                    return rateRuleRepository.save(rule)
                            .doOnNext(saved -> {
                                log.info("Rule created: {}, invalidating cache", saved.getId());
                                snapshotLoader.invalidateAll();
                            });
                });
    }

    public Mono<RateRule> updateRule(Long id, RateRuleRequest request) {
        return rateRuleRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("RateRule", id)))
                .flatMap(existing -> validateTenantAccess(existing.getTenantId()).thenReturn(existing))
                .flatMap(existing -> {
                    existing.setRuleType(request.getRuleType());
                    existing.setCategory(request.getCategory());
                    existing.setDescription(request.getDescription());
                    existing.setPriority(request.getPriority());
                    existing.setConditionExpression(request.getConditionExpression());
                    existing.setValueExpression(request.getValueExpression());

                    return rateRuleRepository.save(existing)
                            .doOnNext(saved -> {
                                log.info("Rule updated: {}, invalidating cache", saved.getId());
                                snapshotLoader.invalidateAll();
                            });
                });
    }

    public Mono<RateRule> getRule(Long id) {
        return rateRuleRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("RateRule", id)))
                .flatMap(rule -> validateTenantAccess(rule.getTenantId()).thenReturn(rule));
    }

    public Flux<RateRule> listRules(Long rateBookId) {
        return rateBookRepository.findById(rateBookId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("RateBook", rateBookId)))
                .flatMapMany(rateBook -> validateTenantAccess(rateBook.getTenantId()).thenMany(rateRuleRepository.findAllByRateBookId(rateBookId)));
    }

    public Mono<Void> deleteRule(Long id) {
        return rateRuleRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("RateRule", id)))
                .flatMap(rule -> validateTenantAccess(rule.getTenantId()).thenReturn(rule))
                .flatMap(rule -> rateRuleRepository.delete(rule))
                .doOnSuccess(v -> snapshotLoader.invalidateAll());
    }

    private Mono<Void> validateTenantAccess(String ownerTenantId) {
        return securityContextService.isAdmin()
                .flatMap(isAdmin -> {
                    if (isAdmin) return Mono.empty();

                    return TenantContext.getTenantId()
                            .flatMap(currentTenantId -> {
                                if (!ownerTenantId.equals(currentTenantId)) {
                                    return Mono.error(new AccessDeniedException("You do not have access to manage rules for tenant: " + ownerTenantId));
                                }
                                return Mono.empty();
                            });
                });
    }
}
