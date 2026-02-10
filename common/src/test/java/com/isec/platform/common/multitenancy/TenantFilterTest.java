package com.isec.platform.common.multitenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TenantFilterTest {

    private TenantProperties tenantProperties;
    private TenantFilter tenantFilter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        tenantProperties = new TenantProperties();
        tenantProperties.setPublicPatterns(List.of(
                "/actuator/**",
                "/api/v1/public/**"
        ));
        tenantFilter = new TenantFilter(tenantProperties);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void doFilterInternal_withTenantHeader_setsTenantContext() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        when(request.getHeader("X-Tenant-Id")).thenReturn("tenant-1");
        
        doAnswer(invocation -> {
            assertThat(TenantContext.getTenantId()).isEqualTo("tenant-1");
            return null;
        }).when(filterChain).doFilter(any(), any());

        // when
        tenantFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertThat(TenantContext.getTenantId()).isNull(); // Should be cleared after filter
    }

    @Test
    void doFilterInternal_withTenantClaim_setsTenantContext() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("tenant_id")).thenReturn("tenant-jwt");
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        doAnswer(invocation -> {
            assertThat(TenantContext.getTenantId()).isEqualTo("tenant-jwt");
            return null;
        }).when(filterChain).doFilter(any(), any());

        // when
        tenantFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertThat(TenantContext.getTenantId()).isNull(); // Should be cleared after filter
    }

    @Test
    void doFilterInternal_missingTenantOnProtectedRoute_returns400() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        // when
        tenantFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(stringWriter.toString()).contains("tenant_id_missing");
        verify(filterChain, never()).doFilter(request, response);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void doFilterInternal_missingTenantOnPublicRoute_proceeds() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/api/v1/public/test");

        // when
        tenantFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_nonApiRoute_proceedsWithoutTenant() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/index.html");

        // when
        tenantFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }
}
