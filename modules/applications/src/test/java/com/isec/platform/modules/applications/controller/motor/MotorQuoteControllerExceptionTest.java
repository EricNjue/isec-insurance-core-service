package com.isec.platform.modules.applications.controller.motor;

import com.isec.platform.common.exception.BusinessException;
import com.isec.platform.common.exception.ErrorResponse;
import com.isec.platform.common.exception.GlobalExceptionHandler;
import com.isec.platform.modules.applications.dto.motor.MpesaInitiationRequest;
import com.isec.platform.modules.applications.service.motor.MotorQuoteOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = MotorQuoteController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration.class
})
@ContextConfiguration(classes = {MotorQuoteController.class, GlobalExceptionHandler.class})
public class MotorQuoteControllerExceptionTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MotorQuoteOrchestrator orchestrator;

    @MockBean
    private com.isec.platform.common.multitenancy.TenantProperties tenantProperties;

    @Test
    void initiatePayment_ShouldReturnBusinessExceptionMessage_WhenOrchestratorThrowsIt() {
        String quoteId = "test-quote-id";
        String errorMessage = "Minimum payment amount is 35% (KES 1000.00) of the total premium";
        
        when(orchestrator.initiatePayment(eq(quoteId), any(MpesaInitiationRequest.class)))
                .thenReturn(Mono.error(new BusinessException(errorMessage)));

        webTestClient.post()
                .uri("/api/v1/motor/quotes/{quoteId}/payments/initiate", quoteId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .consumeWith(result -> {
                    ErrorResponse response = result.getResponseBody();
                    assert response != null;
                    assert errorMessage.equals(response.getMessage());
                    assert response.getStatus() == 400;
                });
    }
}
