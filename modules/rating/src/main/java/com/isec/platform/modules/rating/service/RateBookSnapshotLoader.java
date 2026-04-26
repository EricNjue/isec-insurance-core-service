package com.isec.platform.modules.rating.service;

import com.isec.platform.modules.rating.domain.RateRule;
import com.isec.platform.modules.rating.dto.RateBookDto;
import com.isec.platform.modules.rating.repository.RateBookRepository;
import com.isec.platform.modules.rating.repository.RateRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Loads and caches active RateBook per tenant as a DTO to avoid serialization pitfalls with R2DBC entities.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateBookSnapshotLoader {

    private final RateBookRepository rateBookRepository;
    private final RateRuleRepository rateRuleRepository;

    public static final String RATEBOOK_CACHE = "ratebookSnapshots_v6";

    @Cacheable(cacheNames = RATEBOOK_CACHE, key = "#tenantId")
    public Mono<Snapshot> loadActive(String tenantId) {
        log.debug("Loading active rate book for tenant: {}", tenantId);
        return rateBookRepository.findByTenantIdAndActiveTrue(tenantId)
                .flatMap(rb -> rateRuleRepository.findAllByRateBookId(rb.getId())
                        .collectList()
                        .map(rules -> Snapshot.from(mapToDto(rb, rules))));
    }

    @CacheEvict(cacheNames = RATEBOOK_CACHE, allEntries = true)
    public void invalidateAll() {
        log.info("Invalidated all ratebook snapshots cache");
    }

    private RateBookDto mapToDto(com.isec.platform.modules.rating.domain.RateBook rb, List<RateRule> rules) {
        return RateBookDto.builder()
                .id(rb.getId())
                .tenantId(rb.getTenantId())
                .name(rb.getName())
                .versionName(rb.getVersionName())
                .rules(rules.stream()
                        .map(rule -> RateBookDto.RateRuleDto.builder()
                                .id(rule.getId())
                                .ruleType(rule.getRuleType())
                                .category(rule.getCategory())
                                .description(rule.getDescription())
                                .priority(rule.getPriority())
                                .conditionExpression(rule.getConditionExpression())
                                .valueExpression(rule.getValueExpression())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    public record Snapshot(Long rateBookId, String version, RateBookDto rateBook, String cacheKey) {
        public static Snapshot from(RateBookDto rb) {
            String key = rb.getTenantId() + ":" + rb.getId() + ":" + rb.getVersionName();
            return new Snapshot(rb.getId(), rb.getVersionName(), rb, key);
        }
    }
}
