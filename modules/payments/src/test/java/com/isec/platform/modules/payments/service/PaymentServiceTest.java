package com.isec.platform.modules.payments.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.common.exception.PaymentNotFoundException;
import com.isec.platform.common.exception.PolicyNotFoundException;
import com.isec.platform.modules.certificates.service.CertificateService;
import com.isec.platform.modules.integrations.mpesa.MpesaClient;
import com.isec.platform.modules.integrations.mpesa.repository.MpesaRequestLogRepository;
import com.isec.platform.modules.payments.domain.Payment;
import com.isec.platform.modules.payments.dto.MpesaCallbackRequest;
import com.isec.platform.modules.payments.repository.PaymentRepository;
import com.isec.platform.modules.policies.domain.Policy;
import com.isec.platform.modules.policies.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PolicyService policyService;
    @Mock
    private CertificateService certificateService;
    @Mock
    private MpesaClient mpesaClient;
    @Mock
    private MpesaRequestLogRepository mpesaRequestLogRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentService paymentService;

    private Policy testPolicy;
    private final Long applicationId = 1L;
    private final String phoneNumber = "254712345678";
    private final BigDecimal amount = new BigDecimal("1500");

    @BeforeEach
    void setUp() {
        testPolicy = Policy.builder()
                .applicationId(applicationId)
                .totalAnnualPremium(new BigDecimal("4000"))
                .balance(new BigDecimal("4000"))
                .policyNumber("POL-123")
                .build();
    }

    @Test
    void initiateSTKPush_PolicyNotFound_ThrowsException() {
        when(policyService.getPolicyByApplicationId(applicationId)).thenReturn(Optional.empty());

        assertThrows(PolicyNotFoundException.class, () -> 
            paymentService.initiateSTKPush(applicationId, amount, phoneNumber)
        );
    }

    @Test
    void initiateSTKPush_FirstPaymentBelowThreshold_ThrowsException() {
        when(policyService.getPolicyByApplicationId(applicationId)).thenReturn(Optional.of(testPolicy));
        when(paymentRepository.existsByApplicationIdAndStatus(applicationId, "COMPLETED")).thenReturn(false);

        // 35% of 4000 is 1400. 1000 is below.
        BigDecimal lowAmount = new BigDecimal("1000");

        assertThrows(BusinessException.class, () -> 
            paymentService.initiateSTKPush(applicationId, lowAmount, phoneNumber)
        );
    }

    @Test
    void initiateSTKPush_PolicyFullyPaid_ThrowsException() {
        testPolicy.setBalance(BigDecimal.ZERO);
        when(policyService.getPolicyByApplicationId(applicationId)).thenReturn(Optional.of(testPolicy));
        when(paymentRepository.existsByApplicationIdAndStatus(applicationId, "COMPLETED")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> 
            paymentService.initiateSTKPush(applicationId, amount, phoneNumber)
        );
    }

    @Test
    void initiateSTKPush_MpesaFailure_ThrowsException() {
        when(policyService.getPolicyByApplicationId(applicationId)).thenReturn(Optional.of(testPolicy));
        when(paymentRepository.existsByApplicationIdAndStatus(applicationId, "COMPLETED")).thenReturn(true);
        
        MpesaClient.MpesaResponse errorResponse = new MpesaClient.MpesaResponse("1", "Internal Error", null);
        when(mpesaClient.initiateStkPush(anyString(), any(BigDecimal.class), anyString())).thenReturn(errorResponse);

        assertThrows(RuntimeException.class, () -> 
            paymentService.initiateSTKPush(applicationId, amount, phoneNumber)
        );
    }

    @Test
    void initiateSTKPush_Success() {
        when(policyService.getPolicyByApplicationId(applicationId)).thenReturn(Optional.of(testPolicy));
        when(paymentRepository.existsByApplicationIdAndStatus(applicationId, "COMPLETED")).thenReturn(false);
        
        MpesaClient.MpesaResponse successResponse = new MpesaClient.MpesaResponse("0", "Success", "ws_123");
        when(mpesaClient.initiateStkPush(anyString(), any(BigDecimal.class), anyString())).thenReturn(successResponse);
        
        Payment savedPayment = Payment.builder().id(1L).status("PENDING").build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        Payment result = paymentService.initiateSTKPush(applicationId, amount, phoneNumber);

        assertNotNull(result);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void handleCallback_PaymentNotFound_ThrowsException() {
        MpesaCallbackRequest request = createCallbackRequest("ws_123", 0);
        when(paymentRepository.findByCheckoutRequestId("ws_123")).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> 
            paymentService.handleCallback(request)
        );
    }

    @Test
    void handleCallback_AlreadyCompleted_ReturnsEarly() {
        MpesaCallbackRequest request = createCallbackRequest("ws_123", 0);
        Payment payment = Payment.builder().status("COMPLETED").build();
        when(paymentRepository.findByCheckoutRequestId("ws_123")).thenReturn(Optional.of(payment));

        paymentService.handleCallback(request);

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(policyService, never()).updateBalance(anyLong(), any(BigDecimal.class));
    }

    @Test
    void handleCallback_SuccessfulPayment_UpdatesPolicyAndTriggersCertificate() {
        String checkoutRequestId = "ws_123";
        MpesaCallbackRequest request = createCallbackRequest(checkoutRequestId, 0, "QK12345");
        
        Payment payment = Payment.builder()
                .id(1L)
                .applicationId(applicationId)
                .amount(amount)
                .status("PENDING")
                .checkoutRequestId(checkoutRequestId)
                .build();
        
        when(paymentRepository.findByCheckoutRequestId(checkoutRequestId)).thenReturn(Optional.of(payment));
        when(paymentRepository.existsByMpesaReceiptNumberAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(policyService.updateBalance(eq(applicationId), eq(amount))).thenReturn(testPolicy);

        paymentService.handleCallback(request);

        assertEquals("COMPLETED", payment.getStatus());
        assertEquals("QK12345", payment.getMpesaReceiptNumber());
        verify(paymentRepository, atLeastOnce()).save(payment);
        verify(policyService).updateBalance(applicationId, amount);
        verify(certificateService).processCertificateIssuance(testPolicy, amount);
    }

    @Test
    void handleCallback_DuplicateReceipt_SetsFailed() {
        String checkoutRequestId = "ws_123";
        MpesaCallbackRequest request = createCallbackRequest(checkoutRequestId, 0, "QK12345");
        
        Payment payment = Payment.builder()
                .id(1L)
                .applicationId(applicationId)
                .amount(amount)
                .status("PENDING")
                .checkoutRequestId(checkoutRequestId)
                .build();
        
        when(paymentRepository.findByCheckoutRequestId(checkoutRequestId)).thenReturn(Optional.of(payment));
        when(paymentRepository.existsByMpesaReceiptNumberAndIdNot("QK12345", 1L)).thenReturn(true);

        paymentService.handleCallback(request);

        assertEquals("FAILED", payment.getStatus());
        verify(paymentRepository, atLeastOnce()).save(payment);
        verify(policyService, never()).updateBalance(anyLong(), any(BigDecimal.class));
    }

    @Test
    void handleCallback_FailedPayment_SetsStatusToFailed() {
        String checkoutRequestId = "ws_123";
        MpesaCallbackRequest request = createCallbackRequest(checkoutRequestId, 1032); // Cancelled
        
        Payment payment = Payment.builder()
                .id(1L)
                .applicationId(applicationId)
                .amount(amount)
                .status("PENDING")
                .checkoutRequestId(checkoutRequestId)
                .build();
        
        when(paymentRepository.findByCheckoutRequestId(checkoutRequestId)).thenReturn(Optional.of(payment));

        paymentService.handleCallback(request);

        assertEquals("FAILED", payment.getStatus());
        verify(paymentRepository).save(payment);
        verify(policyService, never()).updateBalance(anyLong(), any(BigDecimal.class));
    }

    private MpesaCallbackRequest createCallbackRequest(String checkoutRequestId, Integer resultCode) {
        return createCallbackRequest(checkoutRequestId, resultCode, null);
    }

    private MpesaCallbackRequest createCallbackRequest(String checkoutRequestId, Integer resultCode, String receiptNumber) {
        MpesaCallbackRequest request = new MpesaCallbackRequest();
        MpesaCallbackRequest.Body body = new MpesaCallbackRequest.Body();
        MpesaCallbackRequest.StkCallback callback = new MpesaCallbackRequest.StkCallback();
        
        callback.setCheckoutRequestId(checkoutRequestId);
        callback.setResultCode(resultCode);
        callback.setResultDesc(resultCode == 0 ? "Success" : "Failed");
        
        if (receiptNumber != null) {
            MpesaCallbackRequest.CallbackMetadata metadata = new MpesaCallbackRequest.CallbackMetadata();
            List<MpesaCallbackRequest.Item> items = new ArrayList<>();
            
            MpesaCallbackRequest.Item item = new MpesaCallbackRequest.Item();
            item.setName("MpesaReceiptNumber");
            item.setValue(receiptNumber);
            items.add(item);
            
            metadata.setItem(items);
            callback.setCallbackMetadata(metadata);
        }
        
        body.setStkCallback(callback);
        request.setBody(body);
        return request;
    }
}
