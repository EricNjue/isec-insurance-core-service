package com.isec.platform.modules.applications.service;

import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.applications.dto.InitiateQuoteRequest;
import com.isec.platform.modules.applications.dto.InitiateQuoteResponse;
import com.isec.platform.modules.applications.dto.QuoteRequest;
import com.isec.platform.modules.applications.dto.QuoteResponse;
import com.isec.platform.modules.customers.service.CustomerService;
import com.isec.platform.modules.documents.service.ApplicationDocumentService;
import com.isec.platform.modules.vehicles.service.UserVehicleService;
import com.isec.platform.modules.rating.dto.PricingResult;
import com.isec.platform.modules.rating.service.PricingEngine;
import com.isec.platform.modules.rating.service.RateBookSnapshotLoader;
import com.isec.platform.common.security.SecurityContextService;
import com.isec.platform.modules.documents.dto.ApplicationDocumentDto;
import com.isec.platform.modules.integrations.common.adapter.InsuranceIntegrationAdapter;
import com.isec.platform.modules.integrations.common.dto.DoubleInsuranceCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock
    private PricingEngine pricingEngine;
    @Mock
    private RateBookSnapshotLoader rateBookSnapshotLoader;
    @Mock
    private ReactiveRedisTemplate<String, Object> redisTemplate;
    @Mock
    private ReactiveValueOperations<String, Object> valueOperations;
    @Mock
    private ApplicationDocumentService documentService;
    @Mock
    private CustomerService customerService;
    @Mock
    private UserVehicleService userVehicleService;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private InsuranceIntegrationAdapter insuranceIntegrationAdapter;

    private QuoteService quoteService;

    @BeforeEach
    void setUp() {
        Map<String, InsuranceIntegrationAdapter> adapters = new HashMap<>();
        adapters.put("SANLAMIntegrationAdapter", insuranceIntegrationAdapter);
        
        quoteService = new QuoteService(
            pricingEngine,
            rateBookSnapshotLoader,
            redisTemplate,
            documentService,
            customerService,
            userVehicleService,
            securityContextService,
            adapters
        );
    }

    @Test
    void initiateQuote_ShouldReturnResponseAndCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        List<ApplicationDocumentDto> mockDocs = List.of(
                ApplicationDocumentDto.builder()
                        .documentType("LOGBOOK")
                        .presignedUrl("http://s3.com/upload")
                        .s3Key("quotes/new/logbook.pdf")
                        .build()
        );
        doReturn(Mono.just(mockDocs)).when(documentService).getOrCreatePresignedUrls(null);
        when(insuranceIntegrationAdapter.checkDoubleInsurance(any())).thenReturn(Mono.just(DoubleInsuranceCheckResponse.builder()
                .hasDuplicate(false)
                .build()));
        when(valueOperations.set(anyString(), any(), any())).thenReturn(Mono.just(true));

        InitiateQuoteRequest request = InitiateQuoteRequest.builder()
                .licensePlateNumber("KAA 123X")
                .build();
        
        Mono<InitiateQuoteResponse> result = quoteService.initiateQuote(request)
                .contextWrite(TenantContext.withTenantId("SANLAM"));

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response.getQuoteId());
                    assertFalse(response.getDocuments().isEmpty());
                    assertEquals("LOGBOOK", response.getDocuments().get(0).getDocumentType());
                })
                .verifyComplete();

        verify(valueOperations).set(contains("quote_init:"), any(), any());
    }

    @Test
    void initiateQuote_WithDuplicate_ShouldReturnEarlyAndNotCache() {
        when(insuranceIntegrationAdapter.checkDoubleInsurance(any())).thenReturn(Mono.just(DoubleInsuranceCheckResponse.builder()
                .hasDuplicate(true)
                .status("double")
                .message("Active cover found")
                .build()));

        InitiateQuoteRequest request = InitiateQuoteRequest.builder()
                .licensePlateNumber("KAA 123X")
                .build();
        
        Mono<InitiateQuoteResponse> result = quoteService.initiateQuote(request)
                .contextWrite(TenantContext.withTenantId("SANLAM"));

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response.getQuoteId());
                    assertTrue(response.getDoubleInsuranceCheck().isHasDuplicate());
                    assertNull(response.getDocuments());
                })
                .verifyComplete();

        verify(documentService, never()).getOrCreatePresignedUrls(any());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void getInitiatedQuote_ShouldReturnCachedResponse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String quoteId = "test-quote-id";
        InitiateQuoteResponse mockResponse = InitiateQuoteResponse.builder()
                .quoteId(quoteId)
                .build();
        when(valueOperations.get("quote_init:" + quoteId)).thenReturn(Mono.just(mockResponse));

        Mono<InitiateQuoteResponse> result = quoteService.getInitiatedQuote(quoteId);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(quoteId, response.getQuoteId());
                })
                .verifyComplete();
    }

    @Test
    void checkDoubleInsurance_ShouldThrowException_WhenNoAdapterFound() {
        when(insuranceIntegrationAdapter.getCompanyCode()).thenReturn("SANLAM");
        
        Mono<DoubleInsuranceCheckResponse> result = quoteService.checkDoubleInsurance("KAA 123X", "CHASSIS123")
                .contextWrite(TenantContext.withTenantId("UNKNOWN"));

        StepVerifier.create(result)
                .expectError(com.isec.platform.common.exception.BusinessException.class)
                .verify();
    }

    @Test
    void calculateQuote_ShouldReturnResponseAndUpsertData() throws Exception {
        QuoteRequest request = createSampleRequest();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.empty());
        doReturn(Mono.just(PricingResult.builder()
                .totalPremium(new BigDecimal("10000"))
                .build())).when(pricingEngine).price(any());
        when(securityContextService.getCurrentUserId()).thenReturn(Mono.just("user123"));
        lenient().when(customerService.createOrUpdateCustomer(anyString(), any())).thenReturn(Mono.empty());
        lenient().when(userVehicleService.saveOrUpdateVehicle(anyString(), any())).thenReturn(Mono.empty());
        when(valueOperations.set(anyString(), any(), any())).thenReturn(Mono.just(true));
        
        com.isec.platform.modules.rating.dto.RateBookDto rbDto = com.isec.platform.modules.rating.dto.RateBookDto.builder()
                .id(1L)
                .tenantId("SANLAM")
                .versionName("v1")
                .build();
        when(rateBookSnapshotLoader.loadActive(any())).thenReturn(Mono.just(RateBookSnapshotLoader.Snapshot.from(rbDto)));

        Mono<QuoteResponse> result = quoteService.calculateQuote(request)
                .contextWrite(TenantContext.withTenantId("SANLAM"));

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("user123-quote", response.getQuoteId());
                })
                .verifyComplete();

        verify(customerService).createOrUpdateCustomer(eq("user123"), any());
        verify(userVehicleService).saveOrUpdateVehicle(eq("user123"), any());
        verify(valueOperations).set(contains("quote_v2:"), any(), any());
    }

    private QuoteRequest createSampleRequest() {
        return QuoteRequest.builder()
                .quoteId("user123-quote")
                .insuranceDetails(QuoteRequest.InsuranceDetails.builder()
                        .category("PRIVATE_CAR")
                        .build())
                .vehicleDetails(QuoteRequest.VehicleDetails.builder()
                        .valuationAmount(new BigDecimal("1000000"))
                        .yearOfManufacture(2020)
                        .makeCode("Toyota")
                        .modelCode("Corolla")
                        .licensePlateNumber("KAA 123X")
                        .build())
                .kycDetails(QuoteRequest.KycDetails.builder()
                        .fullName("John Doe")
                        .email("john@example.com")
                        .phoneNumber("0700000000")
                        .physicalAddress("Nairobi")
                        .build())
                .build();
    }
}
