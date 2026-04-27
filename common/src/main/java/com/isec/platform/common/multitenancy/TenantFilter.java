package com.isec.platform.common.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reactive Filter to extract tenant ID from JWT claim or HTTP header and enforce presence for protected APIs.
 */
@Slf4j
public class TenantFilter implements WebFilter {

    private final TenantProperties tenantProperties;

    public TenantFilter(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TENANT_CLAIM = "tenant_id";

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getRequest().getMethod().name().equals("OPTIONS")) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getPath();
        boolean isPublic = isPublicPath(path);
        boolean isApi = path.startsWith("/api/");

        return extractTenantId(exchange)
                .flatMap(tenantId -> chain.filter(exchange)
                        .contextWrite(TenantContext.withTenantId(tenantId)))
                .switchIfEmpty(Mono.defer(() -> {
                    if (exchange.getResponse().isCommitted()) {
                        return Mono.empty();
                    }
                    if (!isPublic && isApi) {
                        log.warn("Missing tenant identifier for protected API path: {}", path);
                        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                }));
    }

    private Mono<String> extractTenantId(ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(this::extractTenantIdFromSecurityContext)
                .switchIfEmpty(Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(TENANT_HEADER)));
    }

    private Mono<String> extractTenantIdFromSecurityContext(SecurityContext securityContext) {
        return Mono.justOrEmpty(securityContext.getAuthentication())
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .flatMap(this::extractTenantIdFromJwt);
    }

    private Mono<String> extractTenantIdFromJwt(Jwt jwt) {
        return Mono.justOrEmpty(jwt.getClaimAsString(TENANT_CLAIM));
    }

    private boolean isPublicPath(String path) {
        List<String> publicPatterns = tenantProperties.getPublicPatterns();
        return !CollectionUtils.isEmpty(publicPatterns)
                && publicPatterns.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}
