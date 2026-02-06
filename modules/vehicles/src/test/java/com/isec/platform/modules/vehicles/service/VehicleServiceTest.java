package com.isec.platform.modules.vehicles.service;

import com.isec.platform.common.exception.ResourceNotFoundException;
import com.isec.platform.modules.vehicles.domain.VehicleMake;
import com.isec.platform.modules.vehicles.domain.VehicleModel;
import com.isec.platform.modules.vehicles.dto.VehicleMakeRequest;
import com.isec.platform.modules.vehicles.dto.VehicleMakeResponse;
import com.isec.platform.modules.vehicles.dto.VehicleModelRequest;
import com.isec.platform.modules.vehicles.dto.VehicleModelResponse;
import com.isec.platform.modules.vehicles.exception.DuplicateResourceException;
import com.isec.platform.modules.vehicles.repository.VehicleMakeRepository;
import com.isec.platform.modules.vehicles.repository.VehicleModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleServiceTest {

    @Mock
    private VehicleMakeRepository makeRepository;
    @Mock
    private VehicleModelRepository modelRepository;

    @InjectMocks
    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createMake_shouldCreateMake_whenCodeIsUnique() {
        VehicleMakeRequest request = VehicleMakeRequest.builder()
                .code("AUDI")
                .name("Audi")
                .country("Germany")
                .build();

        when(makeRepository.existsByCode("AUDI")).thenReturn(false);
        when(makeRepository.save(any(VehicleMake.class))).thenAnswer(invocation -> {
            VehicleMake make = invocation.getArgument(0);
            make.setId(UUID.randomUUID());
            make.setCreatedAt(LocalDateTime.now());
            make.setUpdatedAt(LocalDateTime.now());
            return make;
        });

        VehicleMakeResponse response = vehicleService.createMake(request);

        assertNotNull(response);
        assertEquals("AUDI", response.getCode());
        verify(makeRepository).save(any(VehicleMake.class));
    }

    @Test
    void createMake_shouldThrowException_whenCodeExists() {
        VehicleMakeRequest request = VehicleMakeRequest.builder().code("AUDI").build();
        when(makeRepository.existsByCode("AUDI")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> vehicleService.createMake(request));
    }

    @Test
    void deleteMake_shouldThrowException_whenHasActiveModels() {
        UUID makeId = UUID.randomUUID();
        VehicleMake make = VehicleMake.builder().id(makeId).build();

        when(makeRepository.findById(makeId)).thenReturn(Optional.of(make));
        when(modelRepository.existsByMakeIdAndActiveTrue(makeId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> vehicleService.deleteMake(makeId));
    }

    @Test
    void createModel_shouldCreateModel_whenValid() {
        UUID makeId = UUID.randomUUID();
        VehicleMake make = VehicleMake.builder().id(makeId).code("AUDI").name("Audi").build();
        VehicleModelRequest request = VehicleModelRequest.builder()
                .makeId(makeId)
                .code("Q7")
                .name("Q7 3.0 TFSI")
                .yearFrom(2015)
                .yearTo(2023)
                .build();

        when(makeRepository.findById(makeId)).thenReturn(Optional.of(make));
        when(modelRepository.existsByMakeIdAndCode(makeId, "Q7")).thenReturn(false);
        when(modelRepository.save(any(VehicleModel.class))).thenAnswer(invocation -> {
            VehicleModel model = invocation.getArgument(0);
            model.setId(UUID.randomUUID());
            model.setCreatedAt(LocalDateTime.now());
            model.setUpdatedAt(LocalDateTime.now());
            return model;
        });

        VehicleModelResponse response = vehicleService.createModel(request);

        assertNotNull(response);
        assertEquals("Q7", response.getCode());
        assertEquals("AUDI", response.getMakeCode());
    }
}
