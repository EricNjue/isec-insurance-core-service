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

import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateBookService {

    private final RateBookRepository rateBookRepository;
    private final SecurityContextService securityContextService;
    private final RateBookSnapshotLoader snapshotLoader;

    public Mono<RateBook> createRateBook(RateBookRequest request) {
        return TenantContext.getTenantId()
                .switchIfEmpty(Mono.error(new IllegalStateException("Tenant context is required to create a RateBook")))
                .flatMap(tenantId -> {
                    RateBook rateBook = RateBook.builder()
                            .name(request.getName())
                            .versionName(request.getVersionName())
                            .effectiveFrom(request.getEffectiveFrom())
                            .effectiveTo(request.getEffectiveTo())
                            .active(request.isActive())
                            .build();
                    rateBook.setTenantId(tenantId);
                    
                    return rateBookRepository.save(rateBook)
                            .doOnNext(saved -> snapshotLoader.invalidateAll());
                });
    }

    public Mono<RateBook> updateRateBook(Long id, RateBookRequest request) {
        return rateBookRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("RateBook", id)))
                .flatMap(existing -> validateTenantAccess(existing.getTenantId()).thenReturn(existing))
                .flatMap(existing -> {
                    existing.setName(request.getName());
                    existing.setVersionName(request.getVersionName());
                    existing.setEffectiveFrom(request.getEffectiveFrom());
                    existing.setEffectiveTo(request.getEffectiveTo());
                    existing.setActive(request.isActive());

                    return rateBookRepository.save(existing)
                            .doOnNext(saved -> snapshotLoader.invalidateAll());
                });
    }

    public Mono<RateBook> getRateBook(Long id) {
        return rateBookRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("RateBook", id)))
                .flatMap(rateBook -> validateTenantAccess(rateBook.getTenantId()).thenReturn(rateBook));
    }

    public Flux<RateBook> listRateBooks() {
        return Mono.zip(securityContextService.isAdmin(), TenantContext.getTenantId())
                .flatMapMany(tuple -> {
                    boolean isAdmin = tuple.getT1();
                    String tenantId = tuple.getT2();

                    if (isAdmin) {
                        return rateBookRepository.findAll();
                    }
                    
                    if (tenantId == null) {
                        return Flux.error(new IllegalStateException("Tenant context missing"));
                    }
                    return rateBookRepository.findAllByTenantId(tenantId);
                });
    }

    public Mono<Void> deleteRateBook(Long id) {
        return rateBookRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("RateBook", id)))
                .flatMap(rateBook -> validateTenantAccess(rateBook.getTenantId()).thenReturn(rateBook))
                .flatMap(rateBook -> rateBookRepository.delete(rateBook))
                .doOnSuccess(v -> snapshotLoader.invalidateAll());
    }

    private Mono<Void> validateTenantAccess(String ownerTenantId) {
        return securityContextService.isAdmin()
                .flatMap(isAdmin -> {
                    if (isAdmin) return Mono.empty();

                    return TenantContext.getTenantId()
                            .flatMap(currentTenantId -> {
                                if (!ownerTenantId.equals(currentTenantId)) {
                                    return Mono.error(new AccessDeniedException("You do not have access to manage RateBooks for tenant: " + ownerTenantId));
                                }
                                return Mono.empty();
                            });
                });
    }
}
