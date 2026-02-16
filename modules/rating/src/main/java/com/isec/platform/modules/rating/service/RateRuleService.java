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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateRuleService {

    private final RateRuleRepository rateRuleRepository;
    private final RateBookRepository rateBookRepository;
    private final SecurityContextService securityContextService;
    private final RateBookSnapshotLoader snapshotLoader;

    @Transactional
    public RateRule createRule(RateRuleRequest request) {
        RateBook rateBook = rateBookRepository.findById(request.getRateBookId())
                .orElseThrow(() -> new ResourceNotFoundException("RateBook", request.getRateBookId()));

        validateTenantAccess(rateBook.getTenantId());

        RateRule rule = RateRule.builder()
                .rateBook(rateBook)
                .ruleType(request.getRuleType())
                .category(request.getCategory())
                .description(request.getDescription())
                .priority(request.getPriority())
                .conditionExpression(request.getConditionExpression())
                .valueExpression(request.getValueExpression())
                .build();

        RateRule saved = rateRuleRepository.save(rule);
        log.info("Rule created: {}, invalidating cache", saved.getId());
        snapshotLoader.invalidateAll(); // Clear cache on rule changes
        return saved;
    }

    @Transactional
    public RateRule updateRule(Long id, RateRuleRequest request) {
        RateRule existing = rateRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RateRule", id));

        validateTenantAccess(existing.getTenantId());

        existing.setRuleType(request.getRuleType());
        existing.setCategory(request.getCategory());
        existing.setDescription(request.getDescription());
        existing.setPriority(request.getPriority());
        existing.setConditionExpression(request.getConditionExpression());
        existing.setValueExpression(request.getValueExpression());

        RateRule saved = rateRuleRepository.save(existing);
        log.info("Rule updated: {}, invalidating cache", saved.getId());
        snapshotLoader.invalidateAll();
        return saved;
    }

    @Transactional(readOnly = true)
    public RateRule getRule(Long id) {
        RateRule rule = rateRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RateRule", id));
        validateTenantAccess(rule.getTenantId());
        return rule;
    }

    @Transactional(readOnly = true)
    public List<RateRule> listRules(Long rateBookId) {
        RateBook rateBook = rateBookRepository.findById(rateBookId)
                .orElseThrow(() -> new ResourceNotFoundException("RateBook", rateBookId));
        validateTenantAccess(rateBook.getTenantId());
        return rateRuleRepository.findAllByRateBookId(rateBookId);
    }

    @Transactional
    public void deleteRule(Long id) {
        RateRule rule = rateRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RateRule", id));
        validateTenantAccess(rule.getTenantId());
        rateRuleRepository.delete(rule);
        snapshotLoader.invalidateAll();
    }

    private void validateTenantAccess(String ownerTenantId) {
        if (securityContextService.isAdmin()) return;

        String currentTenantId = TenantContext.getTenantId();
        if (!ownerTenantId.equals(currentTenantId)) {
            throw new AccessDeniedException("You do not have access to manage rules for tenant: " + ownerTenantId);
        }
    }
}
