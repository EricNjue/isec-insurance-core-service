package com.isec.platform.modules.integrations.quote.sanlam.provider;

import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatus;
import com.isec.platform.modules.integrations.mpesa.model.MpesaPaymentStatusResponse;
import com.isec.platform.modules.integrations.mpesa.sanlam.service.SanlamMpesaProvider;
import com.isec.platform.modules.integrations.premium.sanlam.provider.SanlamPremiumCalculationProvider;
import com.isec.platform.modules.integrations.quote.model.DraftQuoteResponse;
import com.isec.platform.modules.integrations.quote.model.PolicyIssuanceResult;
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
        verify(policyClient).sendDocuments(argThat(request -> request.getQuotSysId().equals(1001L)));
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
}
