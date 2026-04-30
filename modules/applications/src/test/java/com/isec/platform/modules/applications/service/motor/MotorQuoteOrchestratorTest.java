package com.isec.platform.modules.applications.service.motor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.modules.applications.domain.motor.MotorQuoteApplication;
import com.isec.platform.modules.applications.domain.motor.MotorQuoteStatus;
import com.isec.platform.modules.applications.dto.InitiateQuoteResponse;
import com.isec.platform.modules.applications.dto.QuoteRequest;
import com.isec.platform.modules.applications.dto.motor.CalculateMotorPremiumRequest;
import com.isec.platform.modules.applications.dto.motor.MotorQuoteResponse;
import com.isec.platform.modules.applications.mapper.motor.MotorQuoteMapper;
import com.isec.platform.modules.applications.repository.motor.MotorQuoteRepository;
import com.isec.platform.modules.integrations.premium.model.PremiumCalculationResponse;
import com.isec.platform.modules.integrations.quote.provider.PartnerQuoteProvider;
import com.isec.platform.modules.integrations.quote.provider.PartnerQuoteProviderFactory;
import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatus;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatusResponse;
import com.isec.platform.modules.integrations.quote.model.DraftQuoteResponse;
import com.isec.platform.modules.integrations.quote.model.PolicyIssuanceResult;
import com.isec.platform.modules.integrations.quote.provider.QuoteLifecycleCapability;
import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class MotorQuoteOrchestratorTest {

    @Mock
    private MotorQuoteRepository repository;
    @Mock
    private MotorQuoteMapper mapper;
    @Mock
    private PartnerQuoteProviderFactory partnerFactory;
    @Mock
    private PartnerQuoteProvider partnerProvider;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ReactiveRedisTemplate<String, Object> redisTemplate;
    @Mock
    private ReactiveValueOperations<String, Object> valueOperations;

    @InjectMocks
    private MotorQuoteOrchestrator orchestrator;

    private CalculateMotorPremiumRequest calculateRequest;
    private MotorQuoteApplication application;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orchestrator, "minPaymentPercentage", 0.35);
        calculateRequest = CalculateMotorPremiumRequest.builder()
                .quoteId("Q-123")
                .partner(PartnerType.SANLAM)
                .insuranceDetails(QuoteRequest.InsuranceDetails.builder()
                        .category("PRIVATE_CAR")
                        .build())
                .vehicleDetails(QuoteRequest.VehicleDetails.builder()
                        .valuationAmount(new BigDecimal("1000000"))
                        .licensePlateNumber("KDW 123T")
                        .makeCode("Toyota")
                        .modelCode("Camry")
                        .yearOfManufacture(2020)
                        .build())
                .build();

        application = MotorQuoteApplication.builder()
                .quoteId("Q-123")
                .partner(PartnerType.SANLAM)
                .status(MotorQuoteStatus.STARTED)
                .build();
    }

    @Test
    void calculatePremium_ShouldSucceed() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.just(InitiateQuoteResponse.builder().build()));
        when(repository.findByQuoteId(anyString())).thenReturn(Mono.empty());
        when(mapper.toEntity(any())).thenReturn(application);
        when(repository.save(any())).thenReturn(Mono.just(application));
        when(partnerFactory.getProvider(any())).thenReturn(partnerProvider);
        
        PremiumCalculationResponse premiumRes = PremiumCalculationResponse.builder()
                .grossPremium(new BigDecimal("50000"))
                .build();
        when(partnerProvider.calculatePremium(any())).thenReturn(Mono.just(premiumRes));
        
        MotorQuoteResponse response = MotorQuoteResponse.builder()
                .quoteId("Q-123")
                .status(MotorQuoteStatus.PREMIUM_CALCULATED)
                .build();
        when(mapper.toResponse(any())).thenReturn(response);

        StepVerifier.create(orchestrator.calculatePremium(calculateRequest)
                .contextWrite(TenantContext.withTenantId("TEST-TENANT")))
                .expectNextMatches(res -> {
                    verify(repository, atLeastOnce()).save(argThat(app -> "TEST-TENANT".equals(app.getTenantId())));
                    return res.getQuoteId().equals("Q-123") && res.getStatus() == MotorQuoteStatus.PREMIUM_CALCULATED;
                })
                .verifyComplete();

        verify(repository, times(2)).save(any());
        verify(partnerProvider).calculatePremium(any());
    }

    @Test
    void acceptQuote_ShouldCreateDraftQuote_WhenSupported() {
        application.setStatus(MotorQuoteStatus.PREMIUM_CALCULATED);
        when(repository.findByQuoteId("Q-123")).thenReturn(Mono.just(application));
        when(repository.save(any())).thenReturn(Mono.just(application));
        when(partnerFactory.getProvider(any())).thenReturn(partnerProvider);
        when(partnerProvider.supportedCapabilities()).thenReturn(Set.of(QuoteLifecycleCapability.CREATE_DRAFT_QUOTE));
        
        DraftQuoteResponse draftRes = DraftQuoteResponse.builder()
                .draftQuoteRef("REF-123")
                .build();
        when(partnerProvider.createDraftQuote(any())).thenReturn(Mono.just(draftRes));
        
        MotorQuoteResponse response = MotorQuoteResponse.builder()
                .quoteId("Q-123")
                .status(MotorQuoteStatus.DRAFT_QUOTE_CREATED)
                .build();
        when(mapper.toResponse(any())).thenReturn(response);

        StepVerifier.create(orchestrator.acceptQuote("Q-123")
                .contextWrite(TenantContext.withTenantId("TEST-TENANT")))
                .expectNextMatches(res -> res.getStatus() == MotorQuoteStatus.DRAFT_QUOTE_CREATED)
                .verifyComplete();

        verify(partnerProvider).createDraftQuote(any());
    }

    @Test
    void initiatePayment_ShouldSucceed() throws Exception {
        application.setStatus(MotorQuoteStatus.DRAFT_QUOTE_CREATED);
        application.setDraftQuoteResult("{\"draftQuoteRef\":\"REF-123\",\"draftQuoteAmount\":50000,\"clientPhone\":\"0712345678\"}");
        
        when(repository.findByQuoteId("Q-123")).thenReturn(Mono.just(application));
        when(repository.save(any())).thenReturn(Mono.just(application));
        when(partnerFactory.getProvider(any())).thenReturn(partnerProvider);
        
        DraftQuoteResponse draftRes = DraftQuoteResponse.builder()
                .draftQuoteRef("REF-123")
                .draftQuoteAmount(new BigDecimal("50000"))
                .clientPhone("0712345678")
                .build();
        when(objectMapper.readValue(anyString(), eq(DraftQuoteResponse.class))).thenReturn(draftRes);
        
        MpesaInitiatePaymentResponse payRes = MpesaInitiatePaymentResponse.builder()
                .checkoutId("CH-123")
                .build();
        when(partnerProvider.initiatePayment(any())).thenReturn(Mono.just(payRes));
        
        MotorQuoteResponse response = MotorQuoteResponse.builder()
                .quoteId("Q-123")
                .status(MotorQuoteStatus.PAYMENT_INITIATED)
                .build();
        when(mapper.toResponse(any())).thenReturn(response);

        com.isec.platform.modules.applications.dto.motor.MpesaInitiationRequest initReq = new com.isec.platform.modules.applications.dto.motor.MpesaInitiationRequest();
        
        StepVerifier.create(orchestrator.initiatePayment("Q-123", initReq)
                .contextWrite(TenantContext.withTenantId("TEST-TENANT")))
                .expectNextMatches(res -> res.getStatus() == MotorQuoteStatus.PAYMENT_INITIATED)
                .verifyComplete();

        verify(partnerProvider).initiatePayment(any());
    }

    @Test
    void initiatePayment_ShouldUpsertKycDetails_WhenProvided() throws Exception {
        application.setStatus(MotorQuoteStatus.DRAFT_QUOTE_CREATED);
        application.setDraftQuoteResult("{\"draftQuoteRef\":\"REF-123\",\"draftQuoteAmount\":50000,\"clientPhone\":\"0712345678\"}");
        
        when(repository.findByQuoteId("Q-123")).thenReturn(Mono.just(application));
        when(repository.save(any())).thenReturn(Mono.just(application));
        when(partnerFactory.getProvider(any())).thenReturn(partnerProvider);
        
        DraftQuoteResponse draftRes = DraftQuoteResponse.builder()
                .draftQuoteRef("REF-123")
                .draftQuoteAmount(new BigDecimal("50000"))
                .clientPhone("0712345678")
                .build();
        when(objectMapper.readValue(anyString(), eq(DraftQuoteResponse.class))).thenReturn(draftRes);
        
        MpesaInitiatePaymentResponse payRes = MpesaInitiatePaymentResponse.builder()
                .checkoutId("CH-123")
                .build();
        when(partnerProvider.initiatePayment(any())).thenReturn(Mono.just(payRes));
        
        MotorQuoteResponse response = MotorQuoteResponse.builder()
                .quoteId("Q-123")
                .status(MotorQuoteStatus.PAYMENT_INITIATED)
                .build();
        when(mapper.toResponse(any())).thenReturn(response);

        QuoteRequest.KycDetails kyc = QuoteRequest.KycDetails.builder()
                .email("test@test.com")
                .fullName("Test User")
                .phoneNumber("0712345678")
                .build();
        com.isec.platform.modules.applications.dto.motor.MpesaInitiationRequest initReq = com.isec.platform.modules.applications.dto.motor.MpesaInitiationRequest.builder()
                .kycDetails(kyc)
                .build();
        
        StepVerifier.create(orchestrator.initiatePayment("Q-123", initReq)
                .contextWrite(TenantContext.withTenantId("TEST-TENANT")))
                .expectNextMatches(res -> res.getStatus() == MotorQuoteStatus.PAYMENT_INITIATED)
                .verifyComplete();

        verify(mapper).updateKycDetails(eq(application), eq(kyc));
        verify(partnerProvider).initiatePayment(any());
    }

    @Test
    void initiatePayment_ShouldFail_WhenAmountBelowThreshold() throws Exception {
        application.setStatus(MotorQuoteStatus.DRAFT_QUOTE_CREATED);
        application.setDraftQuoteResult("{\"draftQuoteRef\":\"REF-123\",\"draftQuoteAmount\":50000,\"clientPhone\":\"0712345678\"}");
        
        when(repository.findByQuoteId("Q-123")).thenReturn(Mono.just(application));
        when(partnerFactory.getProvider(any())).thenReturn(partnerProvider);
        
        DraftQuoteResponse draftRes = DraftQuoteResponse.builder()
                .draftQuoteRef("REF-123")
                .draftQuoteAmount(new BigDecimal("50000"))
                .clientPhone("0712345678")
                .build();
        when(objectMapper.readValue(anyString(), eq(DraftQuoteResponse.class))).thenReturn(draftRes);

        com.isec.platform.modules.applications.dto.motor.MpesaInitiationRequest initReq = com.isec.platform.modules.applications.dto.motor.MpesaInitiationRequest.builder()
                .amount(10000.0) // 20% of 50000, which is below 35%
                .build();
        
        StepVerifier.create(orchestrator.initiatePayment("Q-123", initReq)
                .contextWrite(TenantContext.withTenantId("TEST-TENANT")))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException && 
                        throwable.getMessage().equals("Minimum payment amount is 35% (KES 17500.00) of the total premium (KES 50000.00)"))
                .verify();
    }

    @Test
    void initiatePayment_ShouldFail_WhenAmountBelowConfiguredThreshold() throws Exception {
        ReflectionTestUtils.setField(orchestrator, "minPaymentPercentage", 0.50);
        application.setStatus(MotorQuoteStatus.DRAFT_QUOTE_CREATED);
        application.setDraftQuoteResult("{\"draftQuoteRef\":\"REF-123\",\"draftQuoteAmount\":50000,\"clientPhone\":\"0712345678\"}");

        when(repository.findByQuoteId("Q-123")).thenReturn(Mono.just(application));
        when(partnerFactory.getProvider(any())).thenReturn(partnerProvider);

        DraftQuoteResponse draftRes = DraftQuoteResponse.builder()
                .draftQuoteRef("REF-123")
                .draftQuoteAmount(new BigDecimal("50000"))
                .clientPhone("0712345678")
                .build();
        when(objectMapper.readValue(anyString(), eq(DraftQuoteResponse.class))).thenReturn(draftRes);

        com.isec.platform.modules.applications.dto.motor.MpesaInitiationRequest initReq = com.isec.platform.modules.applications.dto.motor.MpesaInitiationRequest.builder()
                .amount(20000.0) // 40% of 50000, which is below 50%
                .build();

        StepVerifier.create(orchestrator.initiatePayment("Q-123", initReq)
                .contextWrite(TenantContext.withTenantId("TEST-TENANT")))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().equals("Minimum payment amount is 50% (KES 25000.00) of the total premium (KES 50000.00)"))
                .verify();
    }

    @Test
    void initiatePayment_ShouldSucceed_WhenAmountAboveThreshold() throws Exception {
        application.setStatus(MotorQuoteStatus.DRAFT_QUOTE_CREATED);
        application.setDraftQuoteResult("{\"draftQuoteRef\":\"REF-123\",\"draftQuoteAmount\":50000,\"clientPhone\":\"0712345678\"}");
        
        when(repository.findByQuoteId("Q-123")).thenReturn(Mono.just(application));
        when(repository.save(any())).thenReturn(Mono.just(application));
        when(partnerFactory.getProvider(any())).thenReturn(partnerProvider);
        
        DraftQuoteResponse draftRes = DraftQuoteResponse.builder()
                .draftQuoteRef("REF-123")
                .draftQuoteAmount(new BigDecimal("50000"))
                .clientPhone("0712345678")
                .build();
        when(objectMapper.readValue(anyString(), eq(DraftQuoteResponse.class))).thenReturn(draftRes);
        
        MpesaInitiatePaymentResponse payRes = MpesaInitiatePaymentResponse.builder()
                .checkoutId("CH-123")
                .build();
        when(partnerProvider.initiatePayment(any())).thenReturn(Mono.just(payRes));
        
        MotorQuoteResponse response = MotorQuoteResponse.builder()
                .quoteId("Q-123")
                .status(MotorQuoteStatus.PAYMENT_INITIATED)
                .build();
        when(mapper.toResponse(any())).thenReturn(response);

        com.isec.platform.modules.applications.dto.motor.MpesaInitiationRequest initReq = com.isec.platform.modules.applications.dto.motor.MpesaInitiationRequest.builder()
                .amount(20000.0) // 40% of 50000, which is above 35%
                .build();
        
        StepVerifier.create(orchestrator.initiatePayment("Q-123", initReq)
                .contextWrite(TenantContext.withTenantId("TEST-TENANT")))
                .expectNextMatches(res -> res.getStatus() == MotorQuoteStatus.PAYMENT_INITIATED)
                .verifyComplete();

        verify(partnerProvider).initiatePayment(argThat(req -> req.getAmount() == 20000.0));
    }

    @Test
    void checkPaymentStatus_ShouldUpdateStatus() throws Exception {
        application.setStatus(MotorQuoteStatus.PAYMENT_INITIATED);
        application.setDraftQuoteResult("{\"draftQuoteRef\":\"REF-123\"}");
        application.setPaymentResult("{\"checkoutId\":\"CH-123\"}");
        
        when(repository.findByQuoteId("Q-123")).thenReturn(Mono.just(application));
        when(repository.save(any())).thenReturn(Mono.just(application));
        when(partnerFactory.getProvider(any())).thenReturn(partnerProvider);

        DraftQuoteResponse draftRes = DraftQuoteResponse.builder()
                .draftQuoteRef("REF-123")
                .build();
        when(objectMapper.readValue(eq(application.getDraftQuoteResult()), eq(DraftQuoteResponse.class))).thenReturn(draftRes);
        
        MpesaInitiatePaymentResponse initRes = MpesaInitiatePaymentResponse.builder()
                .checkoutId("CH-123")
                .build();
        // First call will fail Status check and succeed Initiation check
        when(objectMapper.readValue(eq(application.getPaymentResult()), eq(MpesaPaymentStatusResponse.class)))
                .thenThrow(new RuntimeException("Not a status response"));
        when(objectMapper.readValue(eq(application.getPaymentResult()), eq(MpesaInitiatePaymentResponse.class))).thenReturn(initRes);
        
        MpesaPaymentStatusResponse statusRes = MpesaPaymentStatusResponse.builder()
                .status(MpesaPaymentStatus.SUCCESS)
                .checkoutId("CH-123")
                .build();
        when(partnerProvider.checkPaymentStatus(any())).thenReturn(Mono.just(statusRes));
        
        MotorQuoteResponse response = MotorQuoteResponse.builder()
                .quoteId("Q-123")
                .status(MotorQuoteStatus.PAYMENT_SUCCESSFUL)
                .build();
        when(mapper.toResponse(any())).thenReturn(response);

        StepVerifier.create(orchestrator.checkPaymentStatus("Q-123")
                .contextWrite(TenantContext.withTenantId("TEST-TENANT")))
                .expectNextMatches(res -> res.getStatus() == MotorQuoteStatus.PAYMENT_SUCCESSFUL)
                .verifyComplete();

        verify(partnerProvider).checkPaymentStatus(any());
    }

    @Test
    void checkPaymentStatus_ShouldSucceedOnSubsequentCalls() throws Exception {
        application.setStatus(MotorQuoteStatus.PAYMENT_PENDING);
        application.setDraftQuoteResult("{\"draftQuoteRef\":\"REF-123\"}");
        // Previously saved Status response
        application.setPaymentResult("{\"checkoutId\":\"CH-123\",\"status\":\"PENDING\"}");

        when(repository.findByQuoteId("Q-123")).thenReturn(Mono.just(application));
        when(repository.save(any())).thenReturn(Mono.just(application));
        when(partnerFactory.getProvider(any())).thenReturn(partnerProvider);

        DraftQuoteResponse draftRes = DraftQuoteResponse.builder()
                .draftQuoteRef("REF-123")
                .build();
        when(objectMapper.readValue(eq(application.getDraftQuoteResult()), eq(DraftQuoteResponse.class))).thenReturn(draftRes);

        MpesaPaymentStatusResponse previousStatus = MpesaPaymentStatusResponse.builder()
                .checkoutId("CH-123")
                .status(MpesaPaymentStatus.PENDING)
                .build();
        when(objectMapper.readValue(eq(application.getPaymentResult()), eq(MpesaPaymentStatusResponse.class))).thenReturn(previousStatus);

        MpesaPaymentStatusResponse statusRes = MpesaPaymentStatusResponse.builder()
                .status(MpesaPaymentStatus.SUCCESS)
                .checkoutId("CH-123")
                .build();
        when(partnerProvider.checkPaymentStatus(any())).thenReturn(Mono.just(statusRes));

        MotorQuoteResponse response = MotorQuoteResponse.builder()
                .quoteId("Q-123")
                .status(MotorQuoteStatus.PAYMENT_SUCCESSFUL)
                .build();
        when(mapper.toResponse(any())).thenReturn(response);

        StepVerifier.create(orchestrator.checkPaymentStatus("Q-123")
                .contextWrite(TenantContext.withTenantId("TEST-TENANT")))
                .expectNextMatches(res -> res.getStatus() == MotorQuoteStatus.PAYMENT_SUCCESSFUL)
                .verifyComplete();

        verify(partnerProvider).checkPaymentStatus(argThat(req -> req.getCheckoutId().equals("CH-123")));
    }

    @Test
    void issuePolicy_ShouldSucceed() throws Exception {
        application.setStatus(MotorQuoteStatus.PAYMENT_SUCCESSFUL);
        application.setDraftQuoteResult("{\"draftQuoteRef\":\"REF-123\"}");
        application.setPaymentResult("{\"checkoutId\":\"CH-123\",\"status\":\"SUCCESS\"}");

        when(repository.findByQuoteId("Q-123")).thenReturn(Mono.just(application));
        when(repository.save(any())).thenReturn(Mono.just(application));
        when(partnerFactory.getProvider(any())).thenReturn(partnerProvider);
        when(partnerProvider.supportedCapabilities()).thenReturn(Set.of(QuoteLifecycleCapability.ISSUE_POLICY));

        DraftQuoteResponse draftRes = DraftQuoteResponse.builder().draftQuoteRef("REF-123").build();
        when(objectMapper.readValue(eq(application.getDraftQuoteResult()), eq(DraftQuoteResponse.class))).thenReturn(draftRes);

        MpesaPaymentStatusResponse paymentRes = MpesaPaymentStatusResponse.builder().status(MpesaPaymentStatus.SUCCESS).build();
        when(objectMapper.readValue(eq(application.getPaymentResult()), eq(MpesaPaymentStatusResponse.class))).thenReturn(paymentRes);

        PolicyIssuanceResult policyResult = PolicyIssuanceResult.builder()
                .status("POLICY_ISSUED")
                .policyReference("P-123")
                .build();
        when(partnerProvider.issuePolicy(anyString(), any(), any())).thenReturn(Mono.just(policyResult));

        MotorQuoteResponse response = MotorQuoteResponse.builder()
                .quoteId("Q-123")
                .status(MotorQuoteStatus.POLICY_ISSUED)
                .build();
        when(mapper.toResponse(any())).thenReturn(response);

        StepVerifier.create(orchestrator.issuePolicy("Q-123")
                .contextWrite(TenantContext.withTenantId("TEST-TENANT")))
                .expectNextMatches(res -> res.getStatus() == MotorQuoteStatus.POLICY_ISSUED)
                .verifyComplete();

        verify(partnerProvider).issuePolicy(anyString(), any(), any());
    }
}
