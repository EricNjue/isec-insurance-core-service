package com.isec.platform.common.multitenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter to extract tenant ID from JWT claim or HTTP header and enforce presence for protected APIs.
 */
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    private final TenantProperties tenantProperties;

    public TenantFilter(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TENANT_CLAIM = "tenant_id";

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Let CORS preflight pass through untouched
        if (CorsUtils.isPreFlightRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String tenantId = null;

        // 1. Try to get from JWT claim
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt jwt = jwtAuthenticationToken.getToken();
            tenantId = jwt.getClaimAsString(TENANT_CLAIM);
        }

        // 2. Fallback to HTTP header if not in JWT
        if (tenantId == null) {
            tenantId = request.getHeader(TENANT_HEADER);
        }

        // 3. Enforce tenant for protected API paths
        String path = request.getRequestURI();
        List<String> publicPatterns = tenantProperties.getPublicPatterns();
        boolean isPublic = !CollectionUtils.isEmpty(publicPatterns) && publicPatterns.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
        boolean isApi = path.startsWith("/api/");
        if (!isPublic && isApi && tenantId == null) {
            log.warn("Missing tenant identifier for protected API path: {}", path);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"tenant_id_missing\",\"message\":\"Tenant identifier is required via JWT 'tenant_id' claim or 'X-Tenant-Id' header\"}");
            return;
        }

        // 4. Set in context if present
        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
