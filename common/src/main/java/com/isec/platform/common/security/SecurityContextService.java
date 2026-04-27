package com.isec.platform.common.security;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class SecurityContextService {

    /**
     * Retrieves the current authenticated Jwt from the security context.
     *
     * @return Mono containing the Jwt if present and authenticated.
     */
    public Mono<Jwt> getCurrentJwt() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken);
    }

    /**
     * Retrieves the subject (user ID) from the current authenticated Jwt.
     *
     * @return Mono containing the subject if present.
     */
    public Mono<String> getCurrentUserId() {
        return getCurrentJwt().map(Jwt::getSubject);
    }

    /**
     * Retrieves the "name" claim from the current authenticated Jwt.
     *
     * @return Mono containing the name if present.
     */
    public Mono<String> getCurrentUserFullName() {
        return getClaimAsString("name");
    }

    /**
     * Retrieves the "email" claim from the current authenticated Jwt.
     *
     * @return Mono containing the email if present.
     */
    public Mono<String> getCurrentUserEmail() {
        return getClaimAsString("email");
    }

    /**
     * Retrieves a claim as a string from the current authenticated Jwt.
     *
     * @param claim the name of the claim
     * @return Mono containing the claim value if present.
     */
    public Mono<String> getClaimAsString(String claim) {
        return getCurrentJwt()
                .flatMap(jwt -> Mono.justOrEmpty(jwt.getClaimAsString(claim)));
    }

    /**
     * Checks if the current authenticated user has the ADMIN role.
     *
     * @return Mono containing true if the user has the ADMIN role, false otherwise.
     */
    public Mono<Boolean> isAdmin() {
        return getCurrentJwt()
                .map(jwt -> {
                    java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                    if (realmAccess != null) {
                        java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
                        return roles != null && roles.contains("ADMIN");
                    }
                    return false;
                }).defaultIfEmpty(false);
    }
}
