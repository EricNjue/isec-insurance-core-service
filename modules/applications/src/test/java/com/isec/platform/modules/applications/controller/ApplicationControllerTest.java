package com.isec.platform.modules.applications.controller;

import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.dto.ApplicationRequest;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.documents.service.ApplicationDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApplicationControllerTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationDocumentService documentService;

    @InjectMocks
    private ApplicationController applicationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateApplication() {
        // Arrange
        ApplicationRequest request = new ApplicationRequest();
        request.setRegistrationNumber("KAA 001Z");
        request.setVehicleMake("Toyota");
        request.setVehicleModel("Corolla");
        request.setYearOfManufacture(2020);
        request.setVehicleValue(new BigDecimal("2500000"));

        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("test-user");

        Application savedApplication = Application.builder()
                .id(1L)
                .userId("test-user")
                .registrationNumber("KAA 001Z")
                .vehicleMake("Toyota")
                .vehicleModel("Corolla")
                .yearOfManufacture(2020)
                .vehicleValue(new BigDecimal("2500000"))
                .build();

        when(applicationRepository.save(any(Application.class))).thenReturn(savedApplication);
        when(documentService.getOrCreatePresignedUrls(1L)).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<?> response = applicationController.createApplication(request, jwt);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        verify(applicationRepository, times(1)).save(any(Application.class));
        verify(documentService, times(1)).getOrCreatePresignedUrls(1L);
    }
}
