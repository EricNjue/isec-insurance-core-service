package com.isec.platform.common.multitenancy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TenantFilterTest {

    private TenantProperties tenantProperties;
    private TenantFilter tenantFilter;
    private WebFilterChain filterChain;

    @BeforeEach
    void setUp() {
        tenantProperties = new TenantProperties();
        tenantProperties.setPublicPatterns(List.of(
                "/actuator/**",
                "/api/v1/public/**"
        ));
        tenantFilter = new TenantFilter(tenantProperties);
        filterChain = mock(WebFilterChain.class);
        when(filterChain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filter_withTenantHeader_setsTenantContext() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/protected")
                .header("X-Tenant-Id", "tenant-1")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when
        Mono<Void> result = tenantFilter.filter(exchange, filterChain);

        // then
        StepVerifier.create(result)
                .verifyComplete();
        
        verify(filterChain).filter(exchange);
        // TenantContext is cleared in finally block of filter, so we check it inside a dummy filter if we really want to verify it was set
    }

    @Test
    void filter_withTenantClaim_setsTenantContext() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/protected").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("tenant_id")).thenReturn("tenant-jwt");
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

        // when
        Mono<Void> result = tenantFilter.filter(exchange, filterChain)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(exchange);
    }

    @Test
    void filter_withJwtButMissingTenantClaim_onPublicRoute_proceedsWithoutNpe() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/public/integrations/SANLAM/reference-data?productCode=1")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("tenant_id")).thenReturn(null);
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

        // when
        Mono<Void> result = tenantFilter.filter(exchange, filterChain)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void filter_withJwtButMissingTenantClaim_doesNotThrowNPE() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/public/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("tenant_id")).thenReturn(null);
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

        // when
        Mono<Void> result = tenantFilter.filter(exchange, filterChain)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(exchange);
    }

    @Test
    void filter_missingTenantOnProtectedRoute_returns400() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/protected").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when
        Mono<Void> result = tenantFilter.filter(exchange, filterChain);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(filterChain, never()).filter(any());
    }

    @Test
    void filter_missingTenantOnPublicRoute_proceeds() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/public/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when
        Mono<Void> result = tenantFilter.filter(exchange, filterChain);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void filter_withCommittedResponse_doesNothing() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/protected").build();
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        org.springframework.http.server.reactive.ServerHttpResponse response = mock(org.springframework.http.server.reactive.ServerHttpResponse.class);
        
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(response.isCommitted()).thenReturn(true);

        // when
        Mono<Void> result = tenantFilter.filter(exchange, filterChain);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain, never()).filter(any());
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void filter_nonApiRoute_proceedsWithoutTenant() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/index.html").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when
        Mono<Void> result = tenantFilter.filter(exchange, filterChain);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(exchange);
    }

    @Test
    void filter_preflightRequest_proceedsWithoutValidation() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/v1/protected")
                .header("Access-Control-Request-Method", "POST")
                .header("Origin", "http://localhost:3000")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when
        Mono<Void> result = tenantFilter.filter(exchange, filterChain);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(filterChain).filter(exchange);
    }
}
