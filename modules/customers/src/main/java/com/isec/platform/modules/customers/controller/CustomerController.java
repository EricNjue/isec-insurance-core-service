package com.isec.platform.modules.customers.controller;

import com.isec.platform.common.security.SecurityContextService;
import com.isec.platform.modules.customers.domain.Customer;
import com.isec.platform.modules.customers.dto.CustomerRequest;
import com.isec.platform.modules.customers.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;
    private final SecurityContextService securityContextService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public Mono<ResponseEntity<Customer>> createOrUpdateCustomer(
            @Valid @RequestBody CustomerRequest request) {
        return securityContextService.getCurrentUserId()
                .switchIfEmpty(Mono.error(new IllegalStateException("User not authenticated")))
                .flatMap(userId -> {
                    log.info("Request to create/update customer profile for userId: {}", userId);
                    return customerService.createOrUpdateCustomer(userId, request);
                })
                .map(ResponseEntity::ok);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<Customer>> getMyProfile() {
        return securityContextService.getCurrentUserId()
                .switchIfEmpty(Mono.error(new IllegalStateException("User not authenticated")))
                .flatMap(userId -> {
                    log.debug("Fetching customer profile for userId: {}", userId);
                    return customerService.getCustomerByUserId(userId);
                })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Customer>> getCustomerProfile(@PathVariable String userId) {
        log.debug("Admin fetching customer profile for userId: {}", userId);
        return customerService.getCustomerByUserId(userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
