package com.isec.platform.modules.vehicles.controller;

import com.isec.platform.modules.vehicles.dto.VehicleMakeRequest;
import com.isec.platform.modules.vehicles.dto.VehicleMakeResponse;
import com.isec.platform.modules.vehicles.dto.VehicleModelResponse;
import com.isec.platform.modules.vehicles.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Mono<ResponseEntity<VehicleMakeResponse>> createMake(@Valid @RequestBody VehicleMakeRequest request) {
        log.info("Creating vehicle make with code: {}", request.getCode());
        return vehicleService.createMake(request)
                .map(response -> new ResponseEntity<>(response, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<VehicleMakeResponse>> updateMake(@PathVariable UUID id, @Valid @RequestBody VehicleMakeRequest request) {
        log.info("Updating vehicle make with id: {}", id);
        return vehicleService.updateMake(id, request)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<VehicleMakeResponse>> getMake(@PathVariable UUID id) {
        log.debug("Fetching vehicle make with id: {}", id);
        return vehicleService.getMake(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    public Flux<VehicleMakeResponse> getAllMakes(
            @RequestParam(required = false) Boolean active) {
        log.debug("Fetching all vehicle makes, active: {}", active);
        return vehicleService.getAllMakes(active);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Void>> deleteMake(@PathVariable UUID id) {
        log.info("Soft deleting vehicle make with id: {}", id);
        return vehicleService.deleteMake(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @GetMapping("/{makeId}/models")
    public Flux<VehicleModelResponse> getModelsByMakeId(@PathVariable UUID makeId) {
        log.debug("Fetching models for make id: {}", makeId);
        return vehicleService.getModelsByMakeId(makeId);
    }

    @GetMapping("/code/{makeCode}/models")
    public Flux<VehicleModelResponse> getModelsByMakeCode(@PathVariable String makeCode) {
        log.debug("Fetching models for make code: {}", makeCode);
        return vehicleService.getModelsByMakeCode(makeCode);
    }
}
