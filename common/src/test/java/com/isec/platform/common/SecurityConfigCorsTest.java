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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigCorsTest.TestController.class)
@Import({SecurityConfig.class, TenantProperties.class, SecurityProperties.class})
@TestPropertySource(properties = {
    "security.cors.allowed-origins=http://localhost:3000,http://localhost:4200,http://localhost:5173,http://localhost:8080,https://giapiuat-fhapb9ebcvbke6h5.a03.azurefd.net"
})
public class SecurityConfigCorsTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantFilter tenantFilter;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    public void testDefaultCorsOriginsOnPreflight() throws Exception {
        // Test one of the default whitelisted origins
        mockMvc.perform(options("/api/test")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization,content-type,x-auth-token,x-tenant-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "authorization, content-type, x-auth-token, x-tenant-id"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Expose-Headers", "x-auth-token"));
    }

    @Test
    public void testAnotherDefaultCorsOrigin() throws Exception {
        mockMvc.perform(options("/api/test")
                        .header("Origin", "https://giapiuat-fhapb9ebcvbke6h5.a03.azurefd.net")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://giapiuat-fhapb9ebcvbke6h5.a03.azurefd.net"));
    }

    @Test
    public void testCorsHeadersOnDisallowedOrigin() throws Exception {
        mockMvc.perform(options("/api/test")
                        .header("Origin", "http://malicious.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class TestController {
        @GetMapping("/api/test")
        public String test() {
            return "ok";
        }
    }
}
