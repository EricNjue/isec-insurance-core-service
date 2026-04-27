package com.isec.platform.modules.notifications.controller;

import com.isec.platform.modules.notifications.TestApplication;
import com.isec.platform.modules.notifications.service.DeliveryReportService;
import com.isec.platform.modules.notifications.service.SmsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = SmsDeliveryReportController.class)
@ContextConfiguration(classes = {SmsDeliveryReportController.class, TestApplication.class})
class SmsDeliveryReportControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private DeliveryReportService deliveryReportService;

    @MockBean
    private SmsService smsService;

    @MockBean
    private com.isec.platform.common.idempotency.service.IdempotencyService idempotencyService;

    @MockBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @MockBean
    private com.isec.platform.modules.policies.repository.PolicyRepository policyRepository;

    @MockBean
    private com.isec.platform.modules.applications.repository.ApplicationRepository applicationRepository;

    @Test
    void shouldAcceptFormUrlEncodedAndReturn200() {
        Mockito.when(deliveryReportService.handleFormPayload(any())).thenReturn(Mono.empty());

        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt())
                .post()
                .uri("/api/v1/sms/delivery-report")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "ATX123")
                        .with("phoneNumber", "+254700000000")
                        .with("status", "Success")
                        .with("retryCount", "0")
                        .with("networkCode", "63902"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok");

        Mockito.verify(deliveryReportService).handleFormPayload(any());
    }

    @Test
    void shouldAcceptJsonAndReturn200() {
        Mockito.when(deliveryReportService.handleFormPayload(any())).thenReturn(Mono.empty());

        Map<String, String> json = Map.of(
                "id", "ATX123",
                "phoneNumber", "+254700000000",
                "status", "Success"
        );

        webTestClient
                .mutateWith(csrf())
                .mutateWith(mockJwt())
                .post()
                .uri("/api/v1/sms/delivery-report")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok");

        Mockito.verify(deliveryReportService).handleFormPayload(any());
    }
}
