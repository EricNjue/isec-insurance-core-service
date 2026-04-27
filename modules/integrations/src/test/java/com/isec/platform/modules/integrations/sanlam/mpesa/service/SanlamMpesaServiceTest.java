package com.isec.platform.modules.integrations.sanlam.mpesa.service;

import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.modules.integrations.sanlam.mpesa.client.SanlamMpesaClient;
import com.isec.platform.modules.integrations.sanlam.mpesa.dto.request.SanlamStkPushRequest;
import com.isec.platform.modules.integrations.sanlam.mpesa.dto.request.SanlamStkStatusRequest;
import com.isec.platform.modules.integrations.sanlam.mpesa.dto.response.SanlamStkPushResponse;
import com.isec.platform.modules.integrations.sanlam.mpesa.dto.response.SanlamStkStatusResponse;
import com.isec.platform.modules.integrations.sanlam.mpesa.model.MpesaPaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SanlamMpesaServiceTest {

    @Mock
    private SanlamMpesaClient mpesaClient;

    private SanlamMpesaServiceImpl mpesaService;

    @BeforeEach
    void setUp() {
        mpesaService = new SanlamMpesaServiceImpl(mpesaClient);
    }

    @Test
    void initiateStkPush_ShouldCallClientWithCorrectRequest() {
        // Prepare
        String quoteRef = "Q123";
        String phoneNumber = "254712345678";
        Double amount = 100.0;
        SanlamStkPushResponse expectedResponse = SanlamStkPushResponse.builder()
                .status("accepted")
                .checkoutId("ws_123")
                .build();

        when(mpesaClient.initiateStkPush(any(SanlamStkPushRequest.class))).thenReturn(Mono.just(expectedResponse));

        // Execute & Verify
        StepVerifier.create(mpesaService.initiateStkPush(quoteRef, phoneNumber, amount))
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(mpesaClient).initiateStkPush(argThat(request -> 
                request.getQuoteRef().equals(quoteRef) &&
                request.getPhoneNumber().equals(phoneNumber) &&
                request.getAmount().equals(amount)
        ));
    }

    @Test
    void initiateStkPush_WithInvalidPhoneNumber_ShouldThrowException() {
        // Prepare
        String quoteRef = "Q123";
        String phoneNumber = "0712345678"; // Invalid, should start with 254
        Double amount = 100.0;

        // Execute & Verify
        BusinessException exception = assertThrows(BusinessException.class, () -> 
                mpesaService.initiateStkPush(quoteRef, phoneNumber, amount));
        
        assertEquals("Invalid phone number format. Expected 254XXXXXXXXX", exception.getMessage());
        verifyNoInteractions(mpesaClient);
    }

    @Test
    void checkStatus_ShouldMapSuccessResponseCorrectly() {
        // Prepare
        String quoteRef = "Q123";
        String checkoutId = "ws_123";
        SanlamStkStatusResponse sanlamResponse = SanlamStkStatusResponse.builder()
                .status("success")
                .message("Successful")
                .receipt("ABC123DEF")
                .amount(100.0)
                .paidAt("2026-04-27 11:49:37")
                .raw(new SanlamStkStatusResponse.RawResponse(true, "Successful", null, null, null))
                .build();

        when(mpesaClient.checkStkStatus(any(SanlamStkStatusRequest.class))).thenReturn(Mono.just(sanlamResponse));

        // Execute & Verify
        StepVerifier.create(mpesaService.checkStatus(quoteRef, checkoutId))
                .assertNext(status -> {
                    assertEquals(MpesaPaymentStatus.PaymentStatus.SUCCESS, status.getStatus());
                    assertEquals("Successful", status.getMessage());
                    assertEquals("ABC123DEF", status.getReceiptNumber());
                    assertEquals(100.0, status.getAmount());
                })
                .verifyComplete();
    }

    @Test
    void checkStatus_ShouldMapPendingResponseCorrectly() {
        // Prepare
        String quoteRef = "Q123";
        String checkoutId = "ws_123";
        SanlamStkStatusResponse sanlamResponse = SanlamStkStatusResponse.builder()
                .status("failed")
                .message("Processing")
                .raw(new SanlamStkStatusResponse.RawResponse(false, "Failed", null, null, "4999"))
                .build();

        when(mpesaClient.checkStkStatus(any(SanlamStkStatusRequest.class))).thenReturn(Mono.just(sanlamResponse));

        // Execute & Verify
        StepVerifier.create(mpesaService.checkStatus(quoteRef, checkoutId))
                .assertNext(status -> {
                    assertEquals(MpesaPaymentStatus.PaymentStatus.PENDING, status.getStatus());
                })
                .verifyComplete();
    }

    @Test
    void checkStatus_ShouldMapTimeoutResponseCorrectly() {
        // Prepare
        String quoteRef = "Q123";
        String checkoutId = "ws_123";
        SanlamStkStatusResponse sanlamResponse = SanlamStkStatusResponse.builder()
                .status("failed")
                .message("No response")
                .raw(new SanlamStkStatusResponse.RawResponse(false, "Failed", null, null, "1037"))
                .build();

        when(mpesaClient.checkStkStatus(any(SanlamStkStatusRequest.class))).thenReturn(Mono.just(sanlamResponse));

        // Execute & Verify
        StepVerifier.create(mpesaService.checkStatus(quoteRef, checkoutId))
                .assertNext(status -> {
                    assertEquals(MpesaPaymentStatus.PaymentStatus.FAILED, status.getStatus());
                })
                .verifyComplete();
    }
}
