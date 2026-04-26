package com.isec.platform.modules.integrations.registry.controller;

import com.isec.platform.modules.integrations.registry.dto.IntegrationCompanyPublicResponse;
import com.isec.platform.modules.integrations.registry.service.IntegrationCompanyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = IntegrationPublicController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration.class
})
@ContextConfiguration(classes = com.isec.platform.modules.integrations.TestApplication.class)
public class IntegrationPublicControllerTest {

    @MockBean
    private com.isec.platform.common.multitenancy.TenantProperties tenantProperties;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private IntegrationCompanyService service;

    @Test
    void listPublicIntegrations_Success() throws Exception {
        IntegrationCompanyPublicResponse response = IntegrationCompanyPublicResponse.builder()
                .code("SANLAM")
                .name("Sanlam")
                .active(true)
                .build();

        when(service.getPublicIntegrations(anyBoolean())).thenReturn(Flux.just(response));

        webTestClient.get().uri("/api/v1/public/integrations")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].code").isEqualTo("SANLAM")
                .jsonPath("$[0].name").isEqualTo("Sanlam");
    }
}
