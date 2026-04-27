package com.isec.platform.modules.vehicles.controller;

import com.isec.platform.modules.vehicles.dto.VehicleModelRequest;
import com.isec.platform.modules.vehicles.dto.VehicleModelResponse;
import com.isec.platform.modules.vehicles.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles/models")
@RequiredArgsConstructor
@Slf4j
public class VehicleModelController {

    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<VehicleModelResponse>> createModel(@Valid @RequestBody VehicleModelRequest request) {
        log.info("Creating vehicle model with code: {} for make id: {}", request.getCode(), request.getMakeId());
        return vehicleService.createModel(request)
                .map(response -> new ResponseEntity<>(response, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<VehicleModelResponse>> updateModel(@PathVariable UUID id, @Valid @RequestBody VehicleModelRequest request) {
        log.info("Updating vehicle model with id: {}", id);
        return vehicleService.updateModel(id, request)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<VehicleModelResponse>> getModel(@PathVariable UUID id) {
        log.debug("Fetching vehicle model with id: {}", id);
        return vehicleService.getModel(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    public Flux<VehicleModelResponse> getAllModels(
            @RequestParam(required = false) UUID makeId,
            @RequestParam(required = false) String makeCode,
            @RequestParam(required = false) Boolean active) {
        log.debug("Fetching all vehicle models, makeId: {}, makeCode: {}, active: {}", makeId, makeCode, active);
        return vehicleService.getAllModels(makeId, makeCode, active);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Void>> deleteModel(@PathVariable UUID id) {
        log.info("Soft deleting vehicle model with id: {}", id);
        return vehicleService.deleteModel(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
