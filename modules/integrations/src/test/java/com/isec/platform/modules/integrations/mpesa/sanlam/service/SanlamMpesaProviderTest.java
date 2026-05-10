package com.isec.platform.modules.integrations.mpesa.sanlam.service;

import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentRequest;
import com.isec.platform.modules.integrations.mpesa.model.MpesaInitiatePaymentResponse;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatus;
import com.isec.platform.modules.integrations.mpesa.provider.MpesaProviderType;
import com.isec.platform.modules.integrations.mpesa.sanlam.client.SanlamMpesaClient;
import com.isec.platform.modules.integrations.mpesa.sanlam.dto.SanlamStkPushRequest;
import com.isec.platform.modules.integrations.mpesa.sanlam.dto.SanlamStkPushResponse;
import com.isec.platform.modules.integrations.mpesa.sanlam.mapper.SanlamMpesaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SanlamMpesaProviderTest {

    @Mock
    private SanlamMpesaClient mpesaClient;

    private final SanlamMpesaMapper mapper = new SanlamMpesaMapper();

    private SanlamMpesaProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SanlamMpesaProvider(mpesaClient, mapper);
    }

    @Test
    void initiatePayment_ShouldCallClientAndMapResponse() {
        // Prepare
        MpesaInitiatePaymentRequest request = MpesaInitiatePaymentRequest.builder()
                .quoteRef("Q123")
                .phoneNumber("254712345678")
                .amount(100.0)
                .build();

        SanlamStkPushResponse sanlamResponse = SanlamStkPushResponse.builder()
                .status("accepted")
                .checkoutId("ws_123")
                .message("Accepted")
                .build();

        when(mpesaClient.initiateStkPush(any(SanlamStkPushRequest.class))).thenReturn(Mono.just(sanlamResponse));

        // Execute & Verify
        StepVerifier.create(provider.initiatePayment(request))
                .assertNext(response -> {
                    assertEquals(MpesaProviderType.SANLAM, response.getProvider());
                    assertEquals(MpesaPaymentStatus.ACCEPTED, response.getStatus());
                    assertEquals("ws_123", response.getCheckoutId());
                })
                .verifyComplete();
    }

    @Test
    void initiatePayment_ShouldThrowException_WhenStatusIsFailed() {
        // Prepare
        MpesaInitiatePaymentRequest request = MpesaInitiatePaymentRequest.builder()
                .quoteRef("Q123")
                .phoneNumber("254712345678")
                .amount(100.0)
                .build();

        SanlamStkPushResponse sanlamResponse = SanlamStkPushResponse.builder()
                .status("failed")
                .checkoutId("ws_123")
                .message("Failed")
                .build();

        when(mpesaClient.initiateStkPush(any(SanlamStkPushRequest.class))).thenReturn(Mono.just(sanlamResponse));

        // Execute & Verify
        StepVerifier.create(provider.initiatePayment(request))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("[SANLAM] Payment initiated for quoteRef: Q123") &&
                        throwable.getMessage().contains("status: FAILED"))
                .verify();
    }

    @Test
    void initiatePayment_ShouldThrowException_WhenCheckoutIdIsNull() {
        // Prepare
        MpesaInitiatePaymentRequest request = MpesaInitiatePaymentRequest.builder()
                .quoteRef("Q123")
                .phoneNumber("254712345678")
                .amount(100.0)
                .build();

        SanlamStkPushResponse sanlamResponse = SanlamStkPushResponse.builder()
                .status("accepted")
                .checkoutId(null)
                .message("Accepted but no checkoutId")
                .build();

        when(mpesaClient.initiateStkPush(any(SanlamStkPushRequest.class))).thenReturn(Mono.just(sanlamResponse));

        // Execute & Verify
        StepVerifier.create(provider.initiatePayment(request))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        throwable.getMessage().contains("[SANLAM] Payment initiated for quoteRef: Q123") &&
                        throwable.getMessage().contains("checkoutId: null"))
                .verify();
    }
}
