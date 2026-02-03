package com.isec.platform.modules.applications.controller;

import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.dto.ApplicationRequest;
import com.isec.platform.modules.applications.dto.ApplicationResponse;
import com.isec.platform.modules.applications.domain.ApplicationStatus;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationRepository applicationRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody ApplicationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        Application application = Application.builder()
                .userId(jwt.getSubject())
                .registrationNumber(request.getRegistrationNumber())
                .vehicleMake(request.getVehicleMake())
                .vehicleModel(request.getVehicleModel())
                .yearOfManufacture(request.getYearOfManufacture())
                .vehicleValue(request.getVehicleValue())
                .status(ApplicationStatus.DRAFT)
                .build();

        Application saved = applicationRepository.save(application);
        return ResponseEntity.ok(mapToResponse(saved));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<ApplicationResponse> getApplication(@PathVariable Long id) {
        return applicationRepository.findById(id)
                .map(app -> ResponseEntity.ok(mapToResponse(app)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<List<ApplicationResponse>> listApplications(@AuthenticationPrincipal Jwt jwt) {
        List<Application> apps;
        if (jwt.getClaimAsStringList("realm_access.roles") != null && 
            jwt.getClaimAsStringList("realm_access.roles").contains("ADMIN")) {
            apps = applicationRepository.findAll();
        } else {
            apps = applicationRepository.findByUserId(jwt.getSubject());
        }
        return ResponseEntity.ok(apps.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    private ApplicationResponse mapToResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .userId(app.getUserId())
                .registrationNumber(app.getRegistrationNumber())
                .vehicleMake(app.getVehicleMake())
                .vehicleModel(app.getVehicleModel())
                .yearOfManufacture(app.getYearOfManufacture())
                .vehicleValue(app.getVehicleValue())
                .status(app.getStatus())
                .createdAt(app.getCreatedAt())
                .build();
    }
}
