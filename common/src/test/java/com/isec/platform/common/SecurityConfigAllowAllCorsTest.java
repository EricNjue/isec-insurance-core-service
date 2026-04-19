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

    @Test
    public void testAllowLocalhost3000() throws Exception {
        mockMvc.perform(options("/api/test")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Custom-Header"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Custom-Header"));
    }

    @Test
    public void testAllow127001() throws Exception {
        mockMvc.perform(options("/api/test")
                        .header("Origin", "http://127.0.0.1:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:3000"));
    }

    @Test
    public void testAllowLocalhost5173() throws Exception {
        mockMvc.perform(options("/api/test")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    public void testForbiddenOrigin() throws Exception {
        mockMvc.perform(options("/api/test")
                        .header("Origin", "http://any-random-domain.com")
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
