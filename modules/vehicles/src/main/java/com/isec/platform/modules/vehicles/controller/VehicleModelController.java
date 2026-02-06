package com.isec.platform.modules.vehicles.controller;

import com.isec.platform.modules.vehicles.dto.VehicleModelRequest;
import com.isec.platform.modules.vehicles.dto.VehicleModelResponse;
import com.isec.platform.modules.vehicles.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles/models")
@RequiredArgsConstructor
@Slf4j
public class VehicleModelController {

    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehicleModelResponse> createModel(@Valid @RequestBody VehicleModelRequest request) {
        log.info("Creating vehicle model with code: {} for make id: {}", request.getCode(), request.getMakeId());
        return new ResponseEntity<>(vehicleService.createModel(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehicleModelResponse> updateModel(@PathVariable UUID id, @Valid @RequestBody VehicleModelRequest request) {
        log.info("Updating vehicle model with id: {}", id);
        return ResponseEntity.ok(vehicleService.updateModel(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleModelResponse> getModel(@PathVariable UUID id) {
        log.debug("Fetching vehicle model with id: {}", id);
        return ResponseEntity.ok(vehicleService.getModel(id));
    }

    @GetMapping
    public ResponseEntity<Page<VehicleModelResponse>> getAllModels(
            @RequestParam(required = false) UUID makeId,
            @RequestParam(required = false) String makeCode,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        log.debug("Fetching all vehicle models, makeId: {}, makeCode: {}, active: {}", makeId, makeCode, active);
        return ResponseEntity.ok(vehicleService.getAllModels(makeId, makeCode, active, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteModel(@PathVariable UUID id) {
        log.info("Soft deleting vehicle model with id: {}", id);
        vehicleService.deleteModel(id);
        return ResponseEntity.noContent().build();
    }
}
