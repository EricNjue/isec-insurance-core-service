package com.isec.platform.modules.vehicles.service;

import com.isec.platform.common.security.SecurityContextService;
import com.isec.platform.modules.vehicles.domain.UserVehicle;
import com.isec.platform.modules.vehicles.dto.UserVehicleDto;
import com.isec.platform.modules.vehicles.repository.UserVehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class UserVehicleServiceTest {

    private UserVehicleRepository userVehicleRepository;
    private SecurityContextService securityContextService;
    private UserVehicleService userVehicleService;

    @BeforeEach
    void setUp() {
        userVehicleRepository = Mockito.mock(UserVehicleRepository.class);
        securityContextService = Mockito.mock(SecurityContextService.class);
        userVehicleService = new UserVehicleService(userVehicleRepository, securityContextService);
    }

    @Test
    void getMyVehicles_returnsMappedDtos() {
        // given
        when(securityContextService.getCurrentUserId()).thenReturn(Optional.of("user-123"));
        UserVehicle v = UserVehicle.builder()
                .id(UUID.randomUUID())
                .userId("user-123")
                .registrationNumber("KAA 123X")
                .vehicleMake("Toyota")
                .vehicleModel("Corolla")
                .yearOfManufacture(2020)
                .vehicleValue(new BigDecimal("2500000"))
                .chassisNumber("JTDBR32E330123456")
                .engineNumber("1NZ-1234567")
                .build();
        when(userVehicleRepository.findAllByUserId("user-123")).thenReturn(List.of(v));

        // when
        List<UserVehicleDto> result = userVehicleService.getMyVehicles();

        // then
        assertThat(result).hasSize(1);
        UserVehicleDto dto = result.get(0);
        assertThat(dto.getRegistrationNumber()).isEqualTo("KAA 123X");
        assertThat(dto.getVehicleMake()).isEqualTo("Toyota");
        assertThat(dto.getVehicleModel()).isEqualTo("Corolla");
        assertThat(dto.getYearOfManufacture()).isEqualTo(2020);
        assertThat(dto.getVehicleValue()).isEqualByComparingTo("2500000");
        assertThat(dto.getChassisNumber()).isEqualTo("JTDBR32E330123456");
        assertThat(dto.getEngineNumber()).isEqualTo("1NZ-1234567");
    }

    @Test
    void saveOrUpdateVehicle_updatesExisting() {
        // given
        String userId = "user-123";
        UserVehicle existing = UserVehicle.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .registrationNumber("KAA 123X")
                .vehicleMake("Toyota")
                .vehicleModel("Old")
                .yearOfManufacture(2018)
                .vehicleValue(new BigDecimal("1000000"))
                .chassisNumber("OLD-CHASSIS")
                .engineNumber("OLD-ENGINE")
                .build();
        when(userVehicleRepository.findByRegistrationNumberAndUserId("KAA 123X", userId))
                .thenReturn(Optional.of(existing));

        UserVehicleDto dto = UserVehicleDto.builder()
                .registrationNumber("KAA 123X")
                .vehicleMake("Toyota")
                .vehicleModel("Corolla")
                .yearOfManufacture(2020)
                .vehicleValue(new BigDecimal("2500000"))
                .chassisNumber("JTDBR32E330123456")
                .engineNumber("1NZ-1234567")
                .build();

        // when
        userVehicleService.saveOrUpdateVehicle(userId, dto);

        // then
        verify(userVehicleRepository, times(1)).save(any(UserVehicle.class));
        assertThat(existing.getVehicleModel()).isEqualTo("Corolla");
        assertThat(existing.getYearOfManufacture()).isEqualTo(2020);
        assertThat(existing.getVehicleValue()).isEqualByComparingTo("2500000");
        assertThat(existing.getChassisNumber()).isEqualTo("JTDBR32E330123456");
        assertThat(existing.getEngineNumber()).isEqualTo("1NZ-1234567");
    }

    @Test
    void saveOrUpdateVehicle_createsNewWhenAbsent() {
        // given
        String userId = "user-123";
        when(userVehicleRepository.findByRegistrationNumberAndUserId("KBB 987Y", userId))
                .thenReturn(Optional.empty());

        UserVehicleDto dto = UserVehicleDto.builder()
                .registrationNumber("KBB 987Y")
                .vehicleMake("Mazda")
                .vehicleModel("Demio")
                .yearOfManufacture(2019)
                .vehicleValue(new BigDecimal("1500000"))
                .chassisNumber("JM1DE2H2A01234567")
                .engineNumber("ZJ-1234567")
                .build();

        ArgumentCaptor<UserVehicle> captor = ArgumentCaptor.forClass(UserVehicle.class);

        // when
        userVehicleService.saveOrUpdateVehicle(userId, dto);

        // then
        verify(userVehicleRepository, times(1)).save(captor.capture());
        UserVehicle saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getRegistrationNumber()).isEqualTo("KBB 987Y");
        assertThat(saved.getVehicleMake()).isEqualTo("Mazda");
        assertThat(saved.getVehicleModel()).isEqualTo("Demio");
        assertThat(saved.getYearOfManufacture()).isEqualTo(2019);
        assertThat(saved.getVehicleValue()).isEqualByComparingTo("1500000");
        assertThat(saved.getChassisNumber()).isEqualTo("JM1DE2H2A01234567");
        assertThat(saved.getEngineNumber()).isEqualTo("ZJ-1234567");
    }
}
