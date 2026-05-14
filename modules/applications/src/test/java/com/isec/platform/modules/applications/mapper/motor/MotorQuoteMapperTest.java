package com.isec.platform.modules.applications.mapper.motor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.modules.applications.domain.motor.MotorQuoteApplication;
import com.isec.platform.modules.applications.domain.motor.MotorQuoteStatus;
import com.isec.platform.modules.applications.dto.QuoteRequest;
import com.isec.platform.modules.applications.domain.motor.PaymentMethod;
import com.isec.platform.modules.applications.dto.motor.MotorPaymentResult;
import com.isec.platform.modules.applications.dto.motor.MotorQuoteResponse;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationResponse;
import com.isec.platform.modules.integrations.quote.model.DraftQuoteRequest;
import com.isec.platform.modules.integrations.quote.sanlam.service.SanlamTokenService;
import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MotorQuoteMapperTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SanlamTokenService sanlamTokenService;

    @InjectMocks
    private MotorQuoteMapper mapper;

    private MotorQuoteApplication application;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mapper, "minPaymentPercentage", 0.35);
        application = MotorQuoteApplication.builder()
                .quoteId("Q-123")
                .partner(PartnerType.SANLAM)
                .status(MotorQuoteStatus.PREMIUM_CALCULATED)
                .insuranceDetails("{\"category\":\"PRIVATE_CAR\"}")
                .vehicleDetails("{\"licensePlateNumber\":\"KDW 123T\",\"makeCode\":\"Toyota\",\"modelCode\":\"Camry\",\"yearOfManufacture\":2020,\"valuationAmount\":1000000}")
                .premiumResult("{\"grossPremium\":50000,\"basicPremium\":48000}")
                .build();
    }

    @Test
    void toResponse_ShouldIncludeAnnualAndMonthlyPremium() throws Exception {
        PremiumCalculationResponse premiumRes = PremiumCalculationResponse.builder()
                .grossPremium(new BigDecimal("50000"))
                .basicPremium(new BigDecimal("48000"))
                .build();
        
        when(objectMapper.readValue(anyString(), eq(PremiumCalculationResponse.class))).thenReturn(premiumRes);
        when(objectMapper.readValue(anyString(), eq(QuoteRequest.InsuranceDetails.class))).thenReturn(new QuoteRequest.InsuranceDetails());
        when(objectMapper.readValue(anyString(), eq(QuoteRequest.VehicleDetails.class))).thenReturn(new QuoteRequest.VehicleDetails());

        MotorQuoteResponse response = mapper.toResponse(application);

        assertNotNull(response.getPremium());
        assertEquals(new BigDecimal("50000"), response.getPremium().getAnnualPremium());
        // 35% of 50000 is 17500
        assertEquals(new BigDecimal("17500.00"), response.getPremium().getMonthlyPremium());
    }

    @Test
    void toDraftQuoteRequest_ShouldHandleKyc() throws Exception {
        application.setKycDetails("{\"fullName\":\"John Doe\",\"phoneNumber\":\"0712345678\",\"email\":\"john@example.com\",\"idNumber\":\"12345678\"}");
        
        PremiumCalculationResponse premiumRes = PremiumCalculationResponse.builder()
                .grossPremium(new BigDecimal("50000"))
                .basicPremium(new BigDecimal("48000"))
                .build();
        
        when(objectMapper.readValue(anyString(), eq(PremiumCalculationResponse.class))).thenReturn(premiumRes);
        when(objectMapper.readValue(anyString(), eq(QuoteRequest.InsuranceDetails.class))).thenReturn(new QuoteRequest.InsuranceDetails());
        when(objectMapper.readValue(anyString(), eq(QuoteRequest.VehicleDetails.class))).thenReturn(new QuoteRequest.VehicleDetails());
        when(objectMapper.readValue(anyString(), eq(QuoteRequest.KycDetails.class))).thenReturn(QuoteRequest.KycDetails.builder()
                .fullName("John Doe")
                .phoneNumber("0712345678")
                .email("john@example.com")
                .idNumber("12345678")
                .build());
        when(sanlamTokenService.resolveSanlamUserId()).thenReturn(Mono.just(528L));

        StepVerifier.create(mapper.toDraftQuoteRequest(application))
                .expectNextMatches(request -> {
                    assertEquals("John Doe", request.getClientName());
                    assertEquals("0712345678", request.getClientPhone());
                    assertEquals("john@example.com", request.getClientEmail());
                    return true;
                })
                .verifyComplete();
    }
    @Test
    void toResponse_ShouldIncludeManualPaymentInstructions() throws Exception {
        application.setPaymentResult("{\"paymentMethod\":\"MPESA_STK\",\"checkoutId\":\"ws_123\",\"instructions\":[\"Go to M-Pesa\"]}");
        application.setPremiumResult(null);
        application.setInsuranceDetails(null);
        application.setVehicleDetails(null);
        application.setKycDetails(null);
        
        MotorPaymentResult paymentResult = MotorPaymentResult.builder()
                .paymentMethod(PaymentMethod.MPESA_STK)
                .checkoutId("ws_123")
                .instructions(List.of("Go to M-Pesa"))
                .amount(new BigDecimal("50000"))
                .businessNumber("7146151")
                .accountNumber("KDW 123T")
                .build();

        when(objectMapper.readValue(eq(application.getPaymentResult()), eq(MotorPaymentResult.class))).thenReturn(paymentResult);

        MotorQuoteResponse response = mapper.toResponse(application);

        assertNotNull(response.getManualPayment());
        assertEquals(PaymentMethod.MPESA_PAYBILL, response.getManualPayment().getPaymentMethod());
        assertEquals("7146151", response.getManualPayment().getBusinessNumber());
        assertEquals("KDW 123T", response.getManualPayment().getAccountNumber());
        assertFalse(response.getManualPayment().getInstructions().isEmpty());
    }
}
