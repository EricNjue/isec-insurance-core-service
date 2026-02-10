package com.isec.platform.modules.rating.service;

import com.isec.platform.modules.rating.domain.RateBook;
import com.isec.platform.modules.rating.repository.RateBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Loads and caches active RateBook per tenant. Cache key uses tenant and book identity for auditability.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateBookSnapshotLoader {

    private final RateBookRepository rateBookRepository;

    public static final String RATEBOOK_CACHE = "ratebookSnapshots";

    @Cacheable(cacheNames = RATEBOOK_CACHE, key = "#tenantId")
    public Optional<Snapshot> loadActive(String tenantId) {
        Optional<RateBook> rb = rateBookRepository.findActiveByTenantId(tenantId);
        return rb.map(Snapshot::from);
    }

    @CacheEvict(cacheNames = RATEBOOK_CACHE, allEntries = true)
    public void invalidateAll() {
        log.info("Invalidated all ratebook snapshots cache");
    }

    public record Snapshot(Long rateBookId, String version, RateBook rateBook, String cacheKey) {
        public static Snapshot from(RateBook rb) {
            String key = rb.getTenantId() + ":" + rb.getId() + ":" + rb.getVersionName();
            return new Snapshot(rb.getId(), rb.getVersionName(), rb, key);
        }
    }
}
