package com.isec.platform.modules.integrations.registry.controller;

import com.isec.platform.modules.integrations.registry.dto.IntegrationCompanyRequest;
import com.isec.platform.modules.integrations.registry.dto.IntegrationCompanyResponse;
import com.isec.platform.modules.integrations.registry.dto.IntegrationStatusRequest;
import com.isec.platform.modules.integrations.registry.service.IntegrationCompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/integrations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class IntegrationAdminController {

    private final IntegrationCompanyService service;

    @PostMapping
    public Mono<ResponseEntity<IntegrationCompanyResponse>> createIntegration(@Valid @RequestBody IntegrationCompanyRequest request) {
        return service.createIntegration(request)
                .map(response -> new ResponseEntity<>(response, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<IntegrationCompanyResponse>> updateIntegration(
            @PathVariable Long id, 
            @Valid @RequestBody IntegrationCompanyRequest request) {
        return service.updateIntegration(id, request)
                .map(ResponseEntity::ok);
    }

    @PatchMapping("/{id}/status")
    public Mono<ResponseEntity<Void>> updateStatus(
            @PathVariable Long id, 
            @Valid @RequestBody IntegrationStatusRequest request) {
        return service.updateStatus(id, request)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteIntegration(@PathVariable Long id) {
        return service.deleteIntegration(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @GetMapping
    public Flux<IntegrationCompanyResponse> listAllIntegrations() {
        return service.getAllIntegrations();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<IntegrationCompanyResponse>> getIntegration(@PathVariable Long id) {
        return service.getIntegrationById(id)
                .map(ResponseEntity::ok);
    }
}
