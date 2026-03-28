package com.isec.platform.modules.integrations.registry.controller;

import com.isec.platform.modules.integrations.registry.dto.IntegrationCompanyPublicResponse;
import com.isec.platform.modules.integrations.registry.service.IntegrationCompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/integrations")
@RequiredArgsConstructor
public class IntegrationPublicController {

    private final IntegrationCompanyService service;

    @GetMapping
    public ResponseEntity<List<IntegrationCompanyPublicResponse>> listPublicIntegrations(
            @RequestParam(value = "active", required = false, defaultValue = "true") Boolean active) {
        return ResponseEntity.ok(service.getPublicIntegrations(active));
    }
}
