package com.isec.platform.modules.vehicles.service;

import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.common.security.SecurityContextService;
import com.isec.platform.modules.vehicles.domain.UserVehicle;
import com.isec.platform.modules.vehicles.dto.UserVehicleDto;
import com.isec.platform.modules.vehicles.repository.UserVehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserVehicleService {

    private final UserVehicleRepository userVehicleRepository;
    private final SecurityContextService securityContextService;

    @Transactional(readOnly = true)
    public List<UserVehicleDto> getMyVehicles() {
        String userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));

        return userVehicleRepository.findAllByUserId(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void saveOrUpdateVehicle(String userId, UserVehicleDto dto) {
        userVehicleRepository.findByRegistrationNumberAndUserId(dto.getRegistrationNumber(), userId)
                .ifPresentOrElse(
                        existing -> {
                            existing.setVehicleMake(dto.getVehicleMake());
                            existing.setVehicleModel(dto.getVehicleModel());
                            existing.setYearOfManufacture(dto.getYearOfManufacture());
                            existing.setVehicleValue(dto.getVehicleValue());
                            existing.setChassisNumber(dto.getChassisNumber());
                            existing.setEngineNumber(dto.getEngineNumber());
                            userVehicleRepository.save(existing);
                        },
                        () -> {
                            UserVehicle vehicle = UserVehicle.builder()
                                    .userId(userId)
                                    .registrationNumber(dto.getRegistrationNumber())
                                    .vehicleMake(dto.getVehicleMake())
                                    .vehicleModel(dto.getVehicleModel())
                                    .yearOfManufacture(dto.getYearOfManufacture())
                                    .vehicleValue(dto.getVehicleValue())
                                    .chassisNumber(dto.getChassisNumber())
                                    .engineNumber(dto.getEngineNumber())
                                    .build();
                            userVehicleRepository.save(vehicle);
                        }
                );
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
