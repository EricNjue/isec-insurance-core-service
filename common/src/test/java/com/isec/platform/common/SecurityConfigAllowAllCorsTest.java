package com.isec.platform.common;

import com.isec.platform.common.multitenancy.TenantFilter;
import com.isec.platform.common.multitenancy.TenantProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

@WebFluxTest(controllers = SecurityConfigAllowAllCorsTest.TestController.class)
@Import({SecurityConfig.class, TenantProperties.class})
public class SecurityConfigAllowAllCorsTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestConfig {}

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private TenantFilter tenantFilter;

    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @Autowired
    private SecurityConfig securityConfig;

    @Test
    public void testAllowLocalhost8081() throws Exception {
        webTestClient.options().uri("http://localhost/api/test")
                .header("Origin", "http://localhost:8081")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "X-Tenant-Id, Authorization, Content-Type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:8081")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true")
                .expectHeader().valueEquals("Access-Control-Allow-Headers", "X-Tenant-Id, Authorization, Content-Type");
    }

    @Test
    public void testAllowLocalhost3000() throws Exception {
        webTestClient.options().uri("http://localhost/api/test")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    }

    @Test
    public void testAllow127001WithDifferentPorts() throws Exception {
        String[] origins = {
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:8081"
        };
        for (String origin : origins) {
            webTestClient.options().uri("http://localhost/api/test")
                    .header("Origin", origin)
                    .header("Access-Control-Request-Method", "POST")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals("Access-Control-Allow-Origin", origin);
        }
    }

    @Test
    public void testForbiddenOrigin() throws Exception {
        webTestClient.options().uri("http://localhost/api/test")
                .header("Origin", "http://malicious-site.com")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    public void testUrlBasedCorsConfigurationSourceBean() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertThat(source).isNotNull();
        assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);
    }

    @Test
    public void testPublicIntegrationSubpathIsPermittedWithoutAuthentication() {
        webTestClient.get().uri("/api/v1/public/integrations/SANLAM/reference-data")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("ok");
    }

    @RestController
    static class TestController {
        @GetMapping("/api/test")
        public String test() {
            return "ok";
        }

        @GetMapping("/api/v1/public/integrations/SANLAM/reference-data")
        public String publicIntegrationReferenceData() {
            return "ok";
        }
    }
}
