package com.isec.platform.modules.applications.controller;

import com.isec.platform.common.security.SecurityContextService;
import com.isec.platform.modules.applications.dto.ApplicationRequest;
import com.isec.platform.modules.applications.dto.ApplicationResponse;
import com.isec.platform.modules.applications.service.ApplicationService;
import com.isec.platform.modules.rating.service.RatingService;
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
    private ApplicationService applicationService;
    @Mock
    private SecurityContextService securityContextService;

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

        ApplicationResponse expectedResponse = ApplicationResponse.builder()
                .id(1L)
                .userId("test-user")
                .registrationNumber("KAA 001Z")
                .vehicleMake("Toyota")
                .vehicleModel("Corolla")
                .yearOfManufacture(2020)
                .vehicleValue(new BigDecimal("2500000"))
                .build();

        when(applicationService.createApplication(eq(request))).thenReturn(expectedResponse);

        // Act
        ResponseEntity<ApplicationResponse> response = applicationController.createApplication(request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());
        verify(applicationService, times(1)).createApplication(eq(request));
    }

    @Test
    void testGetQuoteShouldCreatePolicy() {
        // Arrange
        Long appId = 1L;
        BigDecimal baseRate = new BigDecimal("0.05");

        RatingService.PremiumBreakdown breakdown = new RatingService.PremiumBreakdown(
                new BigDecimal("50000"),
                new BigDecimal("125"),
                new BigDecimal("100"),
                new BigDecimal("40"),
                new BigDecimal("50265")
        );

        when(applicationService.getQuote(appId, baseRate)).thenReturn(breakdown);

        // Act
        ResponseEntity<RatingService.PremiumBreakdown> response = applicationController.getQuote(appId, baseRate);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(breakdown, response.getBody());
        
        verify(applicationService, times(1)).getQuote(eq(appId), eq(baseRate));
    }
}
