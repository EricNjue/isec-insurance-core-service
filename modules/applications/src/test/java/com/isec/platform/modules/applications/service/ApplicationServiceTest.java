package com.isec.platform.modules.applications.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.common.security.SecurityContextService;
import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.domain.ApplicationStatus;
import com.isec.platform.modules.applications.dto.ApplicationRequest;
import com.isec.platform.modules.applications.dto.ApplicationResponse;
import com.isec.platform.modules.applications.dto.QuoteResponse;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.customers.service.CustomerService;
import com.isec.platform.modules.documents.dto.ApplicationDocumentDto;
import com.isec.platform.modules.documents.service.ApplicationDocumentService;
import com.isec.platform.modules.policies.service.PolicyService;
import com.isec.platform.modules.rating.service.RatingService;
import com.isec.platform.modules.vehicles.service.UserVehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicationDocumentService documentService;
    @Mock
    private RatingService ratingService;
    @Mock
    private QuoteService quoteService;
    @Mock
    private PolicyService policyService;
    @Mock
    private CustomerService customerService;
    @Mock
    private UserVehicleService userVehicleService;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void createApplication_ShouldLinkDocuments() {
        // Arrange
        String userId = "user123";
        ApplicationRequest request = new ApplicationRequest();
        request.setRegistrationNumber("KDW 123T");
        request.setVehicleMake("Toyota");
        request.setVehicleModel("Camry");
        request.setYearOfManufacture(2022);
        request.setVehicleValue(new BigDecimal("2000000"));
        
        List<ApplicationDocumentDto> docs = List.of(
                ApplicationDocumentDto.builder()
                        .documentType("LOGBOOK")
                        .s3Key("quotes/new/logbook.pdf")
                        .presignedUrl("http://s3.com/put")
                        .build()
        );
        request.setDocuments(docs);

        when(securityContextService.getCurrentUserId()).thenReturn(Mono.just(userId));
        when(securityContextService.getCurrentUserFullName()).thenReturn(Mono.just("John Doe"));
        when(securityContextService.getCurrentUserEmail()).thenReturn(Mono.just("john@example.com"));
        
        Application savedApp = Application.builder()
                .id(1L)
                .userId(userId)
                .registrationNumber(request.getRegistrationNumber())
                .status(ApplicationStatus.DRAFT)
                .build();
        
        when(applicationRepository.save(any(Application.class))).thenReturn(Mono.just(savedApp));
        when(documentService.getOrCreatePresignedUrls(any())).thenReturn(Mono.just(List.of()));
        lenient().when(documentService.linkDocumentsToApplication(anyLong(), anyList())).thenReturn(Mono.empty());
        lenient().when(customerService.createOrUpdateCustomer(anyString(), any())).thenReturn(Mono.empty());
        lenient().when(userVehicleService.saveOrUpdateVehicle(anyString(), any())).thenReturn(Mono.empty());

        // Act
        Mono<ApplicationResponse> result = applicationService.createApplication(request);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(1L, response.getId());
                })
                .verifyComplete();
        
        // Verify that linkDocumentsToApplication was called with the correct arguments
        verify(documentService).linkDocumentsToApplication(eq(1L), eq(docs));
        verify(customerService).createOrUpdateCustomer(eq(userId), any());
        verify(userVehicleService).saveOrUpdateVehicle(eq(userId), any());
    }
}
