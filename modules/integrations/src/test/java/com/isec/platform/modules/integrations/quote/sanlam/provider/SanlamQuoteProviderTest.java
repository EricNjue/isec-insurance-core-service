package com.isec.platform.modules.integrations.quote.sanlam.provider;

import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatus;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatusResponse;
import com.isec.platform.modules.integrations.mpesa.sanlam.service.SanlamMpesaProvider;
import com.isec.platform.modules.integrations.premium.sanlam.provider.SanlamPremiumCalculationProvider;
import com.isec.platform.modules.integrations.quote.model.*;
import com.isec.platform.modules.integrations.quote.provider.PartnerType;
import com.isec.platform.modules.integrations.quote.sanlam.client.SanlamDraftQuoteClient;
import com.isec.platform.modules.integrations.quote.sanlam.client.SanlamPolicyClient;
import com.isec.platform.modules.integrations.quote.sanlam.dto.SanlamDraftQuoteResponse;
import com.isec.platform.modules.integrations.quote.sanlam.dto.SanlamEmailResponse;
import com.isec.platform.modules.integrations.quote.sanlam.dto.SanlamUpdateDraftQuoteRequest;
import com.isec.platform.modules.integrations.quote.sanlam.mapper.SanlamDraftQuoteMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SanlamQuoteProviderTest {

    @Mock
    private SanlamDraftQuoteClient client;
    @Mock
    private SanlamPolicyClient policyClient;
    @Mock
    private SanlamDraftQuoteMapper mapper;
    @Mock
    private SanlamPremiumCalculationProvider premiumProvider;
    @Mock
    private SanlamMpesaProvider mpesaProvider;

    private SanlamQuoteProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SanlamQuoteProvider(client, policyClient, mapper, premiumProvider, mpesaProvider);
    }

    @Test
    void issuePolicy_ShouldSucceed() {
        // Arrange
        String quoteId = "Q-123";
        DraftQuoteResponse draftQuote = DraftQuoteResponse.builder()
                .draftQuoteSysId(1001L)
                .draftQuoteRef("REF-123")
                .clientEmail("test@example.com")
                .build();
        MpesaPaymentStatusResponse paymentStatus = MpesaPaymentStatusResponse.builder()
                .status(MpesaPaymentStatus.SUCCESS)
                .checkoutId("CH-123")
                .receiptNumber("REC-123")
                .amount(1000.0)
                .paidAt("2026-05-10 21:34:07")
                .build();

        SanlamDraftQuoteResponse sanlamResponse = new SanlamDraftQuoteResponse();
        sanlamResponse.setQuotSysId(2001L);
        sanlamResponse.setDraftQuoteSysId(1001L);
        sanlamResponse.setDraftQuoteRef("REF-123");

        SanlamEmailResponse emailResponse = new SanlamEmailResponse();
        emailResponse.setMessage("Email queued");
        emailResponse.setQuotSysId(2001L);

        when(mapper.toUpdateDraftQuoteRequest(any(), any())).thenReturn(new SanlamUpdateDraftQuoteRequest());
        when(policyClient.updateDraftQuote(eq(1001L), any())).thenReturn(Mono.just(sanlamResponse));
        when(client.getDraftQuote(eq(1001L))).thenReturn(Mono.just(sanlamResponse));
        when(policyClient.sendDocuments(any())).thenReturn(Mono.just(emailResponse));
        when(mapper.toPolicyIssuanceResult(any(), any())).thenReturn(PolicyIssuanceResult.builder()
                .status("POLICY_ISSUED")
                .policyReference("2001")
                .emailSent(true)
                .build());

        // Act & Assert
        StepVerifier.create(provider.issuePolicy(quoteId, draftQuote, paymentStatus))
                .expectNextMatches(result -> {
                    return result.getStatus().equals("POLICY_ISSUED") &&
                           result.getPolicyReference().equals("2001") &&
                           result.isEmailSent();
                })
                .verifyComplete();

        verify(policyClient).updateDraftQuote(eq(1001L), any());
        verify(policyClient).sendDocuments(argThat(request -> request.getQuotSysId().equals(2001L)));
    }

    @Test
    void issuePolicy_ShouldReturnPaymentSynced_WhenQuotSysIdIsNull() {
        // Arrange
        String quoteId = "Q-123";
        DraftQuoteResponse draftQuote = DraftQuoteResponse.builder()
                .draftQuoteSysId(1001L)
                .draftQuoteRef("REF-123")
                .clientEmail("test@example.com")
                .paymentSummary(QuotePaymentSummary.builder().build())
                .build();
        MpesaPaymentStatusResponse paymentStatus = MpesaPaymentStatusResponse.builder()
                .status(MpesaPaymentStatus.SUCCESS)
                .checkoutId("CH-123")
                .receiptNumber("REC-123")
                .amount(1000.0)
                .paidAt("2026-05-10 21:34:07")
                .build();

        SanlamDraftQuoteResponse sanlamResponse = new SanlamDraftQuoteResponse();
        sanlamResponse.setQuotSysId(null);
        sanlamResponse.setDraftQuoteSysId(1001L);
        sanlamResponse.setDraftQuoteRef("REF-123");
        sanlamResponse.setStatus("draft");

        when(mapper.toUpdateDraftQuoteRequest(any(), any())).thenReturn(new SanlamUpdateDraftQuoteRequest());
        when(policyClient.updateDraftQuote(eq(1001L), any())).thenReturn(Mono.just(sanlamResponse));
        when(client.getDraftQuote(eq(1001L))).thenReturn(Mono.just(sanlamResponse));

        // Act & Assert
        PolicyIssuanceResult result = provider.issuePolicy(quoteId, draftQuote, paymentStatus).block();
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PAYMENT_SYNCED");

        verify(policyClient).updateDraftQuote(eq(1001L), any());
        verify(policyClient, never()).sendDocuments(any());
    }

    @Test
    void issuePolicy_ShouldFail_WhenPaymentNotSuccessful() {
        // Arrange
        String quoteId = "Q-123";
        DraftQuoteResponse draftQuote = DraftQuoteResponse.builder()
                .draftQuoteSysId(1001L)
                .build();
        MpesaPaymentStatusResponse paymentStatus = MpesaPaymentStatusResponse.builder()
                .status(MpesaPaymentStatus.FAILED)
                .build();

        // Act & Assert
        StepVerifier.create(provider.issuePolicy(quoteId, draftQuote, paymentStatus))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void createDraftQuote_ShouldReuseExistingQuote() {
        // Arrange
        DraftQuoteRequest request = DraftQuoteRequest.builder()
                .draftQuoteSysId(1001L)
                .draftQuoteRef("REF-123")
                .provider(PartnerType.SANLAM)
                .draftQuoteAmount(BigDecimal.valueOf(10000))
                .clientName("Test Client")
                .clientPhone("0712345678")
                .clientEmail("test@example.com")
                .clientIdNumber("12345678")
                .draftQuoteUserId(561L)
                .insuranceData(DraftQuoteInsuranceData.builder().build())
                .build();

        SanlamDraftQuoteResponse sanlamResponse = new SanlamDraftQuoteResponse();
        sanlamResponse.setDraftQuoteSysId(1001L);
        sanlamResponse.setDraftQuoteRef("REF-123");
        sanlamResponse.setQuotSysId(2001L);

        when(client.getDraftQuote(eq(1001L))).thenReturn(Mono.just(sanlamResponse));
        when(mapper.toCommonResponse(any())).thenReturn(DraftQuoteResponse.builder()
                .draftQuoteSysId(1001L)
                .draftQuoteRef("REF-123")
                .quotSysId(2001L)
                .build());

        // Act & Assert
        StepVerifier.create(provider.createDraftQuote(request))
                .assertNext(result -> {
                    assertThat(result.getDraftQuoteSysId()).isEqualTo(1001L);
                    assertThat(result.getDraftQuoteRef()).isEqualTo("REF-123");
                    assertThat(result.getQuotSysId()).isEqualTo(2001L);
                })
                .verifyComplete();

        verify(client, never()).createDraftQuote(any());
        verify(client).getDraftQuote(eq(1001L));
    }

    @Test
    void createDraftQuote_ShouldCreateNewQuote_AndImmediatelyRefresh() {
        // Arrange
        DraftQuoteRequest request = DraftQuoteRequest.builder()
                .clientName("Test Client")
                .provider(PartnerType.SANLAM)
                .draftQuoteAmount(BigDecimal.valueOf(10000))
                .clientPhone("0712345678")
                .clientEmail("test@example.com")
                .clientIdNumber("12345678")
                .draftQuoteUserId(561L)
                .insuranceData(DraftQuoteInsuranceData.builder().build())
                .build();

        SanlamDraftQuoteResponse createdResponse = new SanlamDraftQuoteResponse();
        createdResponse.setDraftQuoteSysId(1002L);
        createdResponse.setDraftQuoteRef("REF-456");

        SanlamDraftQuoteResponse refreshedResponse = new SanlamDraftQuoteResponse();
        refreshedResponse.setDraftQuoteSysId(1002L);
        refreshedResponse.setDraftQuoteRef("REF-456");
        refreshedResponse.setQuotSysId(3001L);

        when(mapper.toSanlamRequest(any())).thenReturn(null);
        when(client.createDraftQuote(any())).thenReturn(Mono.just(createdResponse));
        when(client.getDraftQuote(eq(1002L))).thenReturn(Mono.just(refreshedResponse));
        when(mapper.toCommonResponse(any())).thenReturn(DraftQuoteResponse.builder()
                .draftQuoteSysId(1002L)
                .draftQuoteRef("REF-456")
                .quotSysId(3001L)
                .build());

        // Act & Assert
        StepVerifier.create(provider.createDraftQuote(request))
                .assertNext(result -> {
                    assertThat(result.getDraftQuoteSysId()).isEqualTo(1002L);
                    assertThat(result.getDraftQuoteRef()).isEqualTo("REF-456");
                    assertThat(result.getQuotSysId()).isEqualTo(3001L);
                })
                .verifyComplete();

        verify(client).createDraftQuote(any());
        verify(client).getDraftQuote(eq(1002L));
    }

    @Test
    void createDraftQuote_ShouldFail_WhenRefreshFails() {
        // Arrange
        DraftQuoteRequest request = DraftQuoteRequest.builder()
                .draftQuoteSysId(1001L)
                .draftQuoteRef("REF-123")
                .provider(PartnerType.SANLAM)
                .draftQuoteAmount(BigDecimal.valueOf(10000))
                .clientName("Test Client")
                .clientPhone("0712345678")
                .clientEmail("test@example.com")
                .clientIdNumber("12345678")
                .draftQuoteUserId(561L)
                .insuranceData(DraftQuoteInsuranceData.builder().build())
                .build();

        when(client.getDraftQuote(eq(1001L))).thenReturn(Mono.error(new RuntimeException("API error")));

        // Act & Assert
        StepVerifier.create(provider.createDraftQuote(request))
                .expectError(BusinessException.class)
                .verify();
    }
}
