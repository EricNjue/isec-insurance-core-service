package com.isec.platform.modules.applications.service;

import com.isec.platform.common.multitenancy.TenantContext;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private ApplicationDocumentService documentService;
    @Mock
    private CustomerService customerService;
    @Mock
    private UserVehicleService userVehicleService;
    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private QuoteService quoteService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("SANLAM");
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
        when(documentService.getOrCreatePresignedUrls(null)).thenReturn(mockDocs);

        InitiateQuoteResponse response = quoteService.initiateQuote();

        assertNotNull(response.getQuoteId());
        assertFalse(response.getDocuments().isEmpty());
        assertEquals("LOGBOOK", response.getDocuments().get(0).getDocumentType());
        verify(valueOperations).set(eq("quote_init:" + response.getQuoteId()), eq(response), any());
    }

    @Test
    void getInitiatedQuote_ShouldReturnCachedResponse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String quoteId = "test-quote-id";
        InitiateQuoteResponse mockResponse = InitiateQuoteResponse.builder()
                .quoteId(quoteId)
                .build();
        when(valueOperations.get("quote_init:" + quoteId)).thenReturn(mockResponse);

        InitiateQuoteResponse response = quoteService.getInitiatedQuote(quoteId);

        assertNotNull(response);
        assertEquals(quoteId, response.getQuoteId());
    }

    @Test
    void calculateQuote_ShouldReturnResponseAndUpsertData() {
        QuoteRequest request = createSampleRequest();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(pricingEngine.price(any())).thenReturn(PricingResult.builder()
                .totalPremium(new BigDecimal("10000"))
                .build());
        when(securityContextService.getCurrentUserId()).thenReturn(Optional.of("user123"));

        QuoteResponse response = quoteService.calculateQuote(request);

        assertNotNull(response);
        assertEquals("user123-quote", response.getQuoteId());
        verify(customerService).createOrUpdateCustomer(eq("user123"), any());
        verify(userVehicleService).saveOrUpdateVehicle(eq("user123"), any());
        verify(valueOperations).set(eq("quote_v2:" + response.getQuoteId()), eq(response), any());
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
