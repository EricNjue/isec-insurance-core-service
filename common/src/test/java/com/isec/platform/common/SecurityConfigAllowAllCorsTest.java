package com.isec.platform.common;

import com.isec.platform.common.multitenancy.TenantFilter;
import com.isec.platform.common.multitenancy.TenantProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigAllowAllCorsTest.TestController.class)
@Import({SecurityConfig.class, TenantProperties.class})
public class SecurityConfigAllowAllCorsTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantFilter tenantFilter;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private SecurityConfig securityConfig;

    @Test
    public void testAllowLocalhost8081() throws Exception {
        mockMvc.perform(options("/api/test")
                        .header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "X-Tenant-Id, Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Headers", "X-Tenant-Id, Authorization, Content-Type"));
    }

    @Test
    public void testAllowLocalhost3000() throws Exception {
        mockMvc.perform(options("/api/test")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    public void testAllow127001WithDifferentPorts() throws Exception {
        String[] origins = {
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:8081"
        };
        for (String origin : origins) {
            mockMvc.perform(options("/api/test")
                            .header("Origin", origin)
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
    }

    @Test
    public void testForbiddenOrigin() throws Exception {
        mockMvc.perform(options("/api/test")
                        .header("Origin", "http://malicious-site.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUrlBasedCorsConfigurationSourceBean() {
        UrlBasedCorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertThat(source).isNotNull();
    }

    @RestController
    static class TestController {
        @GetMapping("/api/test")
        public String test() {
            return "ok";
        }
    }
}
