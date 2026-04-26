package com.isec.platform.modules.rating.service;

import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.rating.dto.AddonDto;
import com.isec.platform.modules.rating.dto.RateBookDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddonService {

    private final RateBookSnapshotLoader rateBookSnapshotLoader;

    public Flux<AddonDto> getAvailableAddons() {
        return com.isec.platform.common.multitenancy.TenantContext.getTenantId()
                .switchIfEmpty(Mono.error(new IllegalStateException("Attempted to fetch addons without tenant context")))
                .flatMapMany(tenantId -> rateBookSnapshotLoader.loadActive(tenantId)
                        .flatMapMany(snapshot -> Flux.fromIterable(snapshot.rateBook().getRules()))
                        .filter(r -> r.getRuleType() == com.isec.platform.modules.rating.domain.RuleType.ADDON)
                        .map(this::mapToDto));
    }

    public Flux<AddonDto> getAddonsByCategory(String category) {
        return getAvailableAddons()
                .filter(a -> a.getCategory().equalsIgnoreCase(category));
    }

    private AddonDto mapToDto(RateBookDto.RateRuleDto rule) {
        return AddonDto.builder()
                .id(rule.getId())
                .name(rule.getDescription())
                .description(rule.getDescription())
                .category(rule.getCategory())
                .build();
    }
}
