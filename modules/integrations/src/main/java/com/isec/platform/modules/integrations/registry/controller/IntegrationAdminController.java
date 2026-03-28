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

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/integrations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class IntegrationAdminController {

    private final IntegrationCompanyService service;

    @PostMapping
    public ResponseEntity<IntegrationCompanyResponse> createIntegration(@Valid @RequestBody IntegrationCompanyRequest request) {
        return new ResponseEntity<>(service.createIntegration(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IntegrationCompanyResponse> updateIntegration(
            @PathVariable Long id, 
            @Valid @RequestBody IntegrationCompanyRequest request) {
        return ResponseEntity.ok(service.updateIntegration(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id, 
            @Valid @RequestBody IntegrationStatusRequest request) {
        service.updateStatus(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIntegration(@PathVariable Long id) {
        service.deleteIntegration(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<IntegrationCompanyResponse>> listAllIntegrations() {
        return ResponseEntity.ok(service.getAllIntegrations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IntegrationCompanyResponse> getIntegration(@PathVariable Long id) {
        return ResponseEntity.ok(service.getIntegrationById(id));
    }
}
