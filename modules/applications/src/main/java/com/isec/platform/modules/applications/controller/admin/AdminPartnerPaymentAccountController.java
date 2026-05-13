package com.isec.platform.modules.applications.controller.admin;

import com.isec.platform.modules.applications.domain.motor.PartnerPaymentAccount;
import com.isec.platform.modules.applications.service.motor.PartnerPaymentAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/partner-payment-accounts")
@RequiredArgsConstructor
@Slf4j
public class AdminPartnerPaymentAccountController {

    private final PartnerPaymentAccountService service;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<ResponseEntity<PartnerPaymentAccount>> create(@RequestBody PartnerPaymentAccount account) {
        log.info("Admin request to create partner payment account");
        return service.save(account)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Flux<PartnerPaymentAccount> findAll() {
        log.info("Admin request to list all partner payment accounts");
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<ResponseEntity<PartnerPaymentAccount>> findById(@PathVariable UUID id) {
        log.info("Admin request to get partner payment account: {}", id);
        return service.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<ResponseEntity<PartnerPaymentAccount>> update(@PathVariable UUID id, @RequestBody PartnerPaymentAccount account) {
        log.info("Admin request to update partner payment account: {}", id);
        account.setId(id);
        return service.save(account)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable UUID id) {
        log.info("Admin request to delete partner payment account: {}", id);
        return service.deleteById(id)
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }
}
