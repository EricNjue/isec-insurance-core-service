package com.isec.platform.modules.rating.service;

import com.isec.platform.common.exception.ResourceNotFoundException;
import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.common.security.SecurityContextService;
import com.isec.platform.modules.rating.domain.RateBook;
import com.isec.platform.modules.rating.dto.RateBookRequest;
import com.isec.platform.modules.rating.repository.RateBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateBookService {

    private final RateBookRepository rateBookRepository;
    private final SecurityContextService securityContextService;
    private final RateBookSnapshotLoader snapshotLoader;

    @Transactional
    public RateBook createRateBook(RateBookRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required to create a RateBook");
        }

        RateBook rateBook = RateBook.builder()
                .name(request.getName())
                .versionName(request.getVersionName())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .active(request.isActive())
                .build();
        
        // TenantBaseEntity will auto-populate tenantId from context via @PrePersist
        RateBook saved = rateBookRepository.save(rateBook);
        snapshotLoader.invalidateAll();
        return saved;
    }

    @Transactional
    public RateBook updateRateBook(Long id, RateBookRequest request) {
        RateBook existing = rateBookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RateBook", id));

        validateTenantAccess(existing.getTenantId());

        existing.setName(request.getName());
        existing.setVersionName(request.getVersionName());
        existing.setEffectiveFrom(request.getEffectiveFrom());
        existing.setEffectiveTo(request.getEffectiveTo());
        existing.setActive(request.isActive());

        RateBook saved = rateBookRepository.save(existing);
        snapshotLoader.invalidateAll();
        return saved;
    }

    @Transactional(readOnly = true)
    public RateBook getRateBook(Long id) {
        RateBook rateBook = rateBookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RateBook", id));
        validateTenantAccess(rateBook.getTenantId());
        return rateBook;
    }

    @Transactional(readOnly = true)
    public List<RateBook> listRateBooks() {
        String tenantId = TenantContext.getTenantId();
        boolean isAdmin = securityContextService.isAdmin();

        if (isAdmin) {
            return rateBookRepository.findAll();
        }
        
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context missing");
        }
        return rateBookRepository.findAllByTenantId(tenantId);
    }

    @Transactional
    public void deleteRateBook(Long id) {
        RateBook rateBook = rateBookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RateBook", id));
        validateTenantAccess(rateBook.getTenantId());
        rateBookRepository.delete(rateBook);
        snapshotLoader.invalidateAll();
    }

    private void validateTenantAccess(String ownerTenantId) {
        if (securityContextService.isAdmin()) return;

        String currentTenantId = TenantContext.getTenantId();
        if (!ownerTenantId.equals(currentTenantId)) {
            throw new AccessDeniedException("You do not have access to manage RateBooks for tenant: " + ownerTenantId);
        }
    }
}
