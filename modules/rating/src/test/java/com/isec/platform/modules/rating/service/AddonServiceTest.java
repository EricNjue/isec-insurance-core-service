package com.isec.platform.modules.rating.service;

import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.rating.dto.AddonDto;
import com.isec.platform.modules.rating.dto.RateBookDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AddonServiceTest {

    private RateBookSnapshotLoader snapshotLoader;
    private AddonService addonService;

    @BeforeEach
    void setUp() {
        snapshotLoader = Mockito.mock(RateBookSnapshotLoader.class);
        addonService = new AddonService(snapshotLoader);
    }

    @Test
    void getAvailableAddons_returnsOnlyAddons() {
        // given
        String tenantId = "TENANT1";

        RateBookDto.RateRuleDto addonRule = RateBookDto.RateRuleDto.builder()
                .id(1L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.ADDON)
                .category("PRIVATE_CAR")
                .description("Windscreen")
                .build();

        RateBookDto.RateRuleDto baseRule = RateBookDto.RateRuleDto.builder()
                .id(2L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.BASE_PREMIUM)
                .category("PRIVATE_CAR")
                .description("Base")
                .build();

        RateBookDto rb = RateBookDto.builder()
                .id(1L)
                .tenantId(tenantId)
                .rules(List.of(addonRule, baseRule))
                .build();

        when(snapshotLoader.loadActive(tenantId)).thenReturn(Mono.just(RateBookSnapshotLoader.Snapshot.from(rb)));

        // when & then
        addonService.getAvailableAddons()
                .contextWrite(TenantContext.withTenantId(tenantId))
                .as(StepVerifier::create)
                .consumeNextWith(addon -> {
                    assertThat(addon.getName()).isEqualTo("Windscreen");
                })
                .verifyComplete();
    }

    @Test
    void getAddonsByCategory_filtersCorrectly() {
        // given
        String tenantId = "TENANT1";

        RateBookDto.RateRuleDto addon1 = RateBookDto.RateRuleDto.builder()
                .id(1L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.ADDON)
                .category("PRIVATE_CAR")
                .description("Addon 1")
                .build();

        RateBookDto.RateRuleDto addon2 = RateBookDto.RateRuleDto.builder()
                .id(2L)
                .ruleType(com.isec.platform.modules.rating.domain.RuleType.ADDON)
                .category("COMMERCIAL")
                .description("Addon 2")
                .build();

        RateBookDto rb = RateBookDto.builder()
                .id(1L)
                .tenantId(tenantId)
                .rules(List.of(addon1, addon2))
                .build();

        when(snapshotLoader.loadActive(tenantId)).thenReturn(Mono.just(RateBookSnapshotLoader.Snapshot.from(rb)));

        // when & then
        addonService.getAddonsByCategory("COMMERCIAL")
                .contextWrite(TenantContext.withTenantId(tenantId))
                .as(StepVerifier::create)
                .consumeNextWith(addon -> {
                    assertThat(addon.getName()).isEqualTo("Addon 2");
                })
                .verifyComplete();
    }
}
