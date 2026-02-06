package com.isec.platform.modules.vehicles.controller;

import com.isec.platform.modules.vehicles.dto.VehicleMakeRequest;
import com.isec.platform.modules.vehicles.dto.VehicleMakeResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles/makes")
@RequiredArgsConstructor
@Slf4j
public class VehicleMakeController {

    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehicleMakeResponse> createMake(@Valid @RequestBody VehicleMakeRequest request) {
        log.info("Creating vehicle make with code: {}", request.getCode());
        return new ResponseEntity<>(vehicleService.createMake(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehicleMakeResponse> updateMake(@PathVariable UUID id, @Valid @RequestBody VehicleMakeRequest request) {
        log.info("Updating vehicle make with id: {}", id);
        return ResponseEntity.ok(vehicleService.updateMake(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleMakeResponse> getMake(@PathVariable UUID id) {
        log.debug("Fetching vehicle make with id: {}", id);
        return ResponseEntity.ok(vehicleService.getMake(id));
    }

    @GetMapping
    public ResponseEntity<Page<VehicleMakeResponse>> getAllMakes(
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        log.debug("Fetching all vehicle makes, active: {}", active);
        return ResponseEntity.ok(vehicleService.getAllMakes(active, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMake(@PathVariable UUID id) {
        log.info("Soft deleting vehicle make with id: {}", id);
        vehicleService.deleteMake(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{makeId}/models")
    public ResponseEntity<List<VehicleModelResponse>> getModelsByMakeId(@PathVariable UUID makeId) {
        log.debug("Fetching models for make id: {}", makeId);
        return ResponseEntity.ok(vehicleService.getModelsByMakeId(makeId));
    }

    @GetMapping("/code/{makeCode}/models")
    public ResponseEntity<List<VehicleModelResponse>> getModelsByMakeCode(@PathVariable String makeCode) {
        log.debug("Fetching models for make code: {}", makeCode);
        return ResponseEntity.ok(vehicleService.getModelsByMakeCode(makeCode));
    }
}
