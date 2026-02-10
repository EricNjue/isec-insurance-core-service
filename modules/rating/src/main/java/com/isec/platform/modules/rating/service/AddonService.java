package com.isec.platform.modules.rating.service;

import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.rating.dto.AddonDto;
import com.isec.platform.modules.rating.dto.RateBookDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddonService {

    private final RateBookSnapshotLoader rateBookSnapshotLoader;

    public List<AddonDto> getAvailableAddons() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.warn("Attempted to fetch addons without tenant context");
            return Collections.emptyList();
        }

        // loadActive returns Snapshot (nullable)
        RateBookSnapshotLoader.Snapshot snapshot = rateBookSnapshotLoader.loadActive(tenantId);
        
        if (snapshot == null) {
            return Collections.emptyList();
        }

        RateBookDto rateBook = snapshot.rateBook();
        
        return rateBook.getRules().stream()
                .filter(r -> r.getRuleType() == com.isec.platform.modules.rating.domain.RuleType.ADDON)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<AddonDto> getAddonsByCategory(String category) {
        return getAvailableAddons().stream()
                .filter(a -> a.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
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
