package com.isec.platform.modules.vehicles.controller;

import com.isec.platform.modules.vehicles.dto.UserVehicleDto;
import com.isec.platform.modules.vehicles.service.UserVehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/vehicles/my-vehicles")
@RequiredArgsConstructor
@Slf4j
public class UserVehicleController {

    private final UserVehicleService userVehicleService;

    @GetMapping
    @PreAuthorize("hasRole('RETAIL_USER')")
    public Flux<UserVehicleDto> getMyVehicles() {
        log.info("Fetching vehicles for current user");
        return userVehicleService.getMyVehicles();
    }
}
