package com.isec.platform.modules.vehicles.service;

import com.isec.platform.common.security.SecurityContextService;
import com.isec.platform.modules.vehicles.domain.UserVehicle;
import com.isec.platform.modules.vehicles.dto.UserVehicleDto;
import com.isec.platform.modules.vehicles.repository.UserVehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserVehicleService {

    private final UserVehicleRepository userVehicleRepository;
    private final SecurityContextService securityContextService;

    public Flux<UserVehicleDto> getMyVehicles() {
        return securityContextService.getCurrentUserId()
                .switchIfEmpty(Mono.error(new IllegalStateException("User not authenticated")))
                .flatMapMany(userVehicleRepository::findAllByUserId)
                .map(this::mapToDto);
    }

    public Mono<UserVehicle> saveOrUpdateVehicle(String userId, UserVehicleDto dto) {
        return userVehicleRepository.findByRegistrationNumberAndUserId(dto.getRegistrationNumber(), userId)
                .flatMap(existing -> {
                    existing.setVehicleMake(dto.getVehicleMake());
                    existing.setVehicleModel(dto.getVehicleModel());
                    existing.setYearOfManufacture(dto.getYearOfManufacture());
                    existing.setVehicleValue(dto.getVehicleValue());
                    existing.setChassisNumber(dto.getChassisNumber());
                    existing.setEngineNumber(dto.getEngineNumber());
                    return userVehicleRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    UserVehicle vehicle = UserVehicle.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .registrationNumber(dto.getRegistrationNumber())
                            .vehicleMake(dto.getVehicleMake())
                            .vehicleModel(dto.getVehicleModel())
                            .yearOfManufacture(dto.getYearOfManufacture())
                            .vehicleValue(dto.getVehicleValue())
                            .chassisNumber(dto.getChassisNumber())
                            .engineNumber(dto.getEngineNumber())
                            .build();
                    return userVehicleRepository.save(vehicle);
                }));
    }

    private UserVehicleDto mapToDto(UserVehicle vehicle) {
        return UserVehicleDto.builder()
                .id(vehicle.getId())
                .registrationNumber(vehicle.getRegistrationNumber())
                .vehicleMake(vehicle.getVehicleMake())
                .vehicleModel(vehicle.getVehicleModel())
                .yearOfManufacture(vehicle.getYearOfManufacture())
                .vehicleValue(vehicle.getVehicleValue())
                .chassisNumber(vehicle.getChassisNumber())
                .engineNumber(vehicle.getEngineNumber())
                .build();
    }
}
