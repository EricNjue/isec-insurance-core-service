package com.isec.platform.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityContextService {

    /**
     * Retrieves the current authenticated Jwt from the security context.
     *
     * @return Optional containing the Jwt if present and authenticated, empty otherwise.
     */
    public Optional<Jwt> getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return Optional.of(jwtAuthenticationToken.getToken());
        }
        return Optional.empty();
    }

    /**
     * Retrieves the subject (user ID) from the current authenticated Jwt.
     *
     * @return Optional containing the subject if present, empty otherwise.
     */
    public Optional<String> getCurrentUserId() {
        return getCurrentJwt().map(Jwt::getSubject);
    }

    /**
     * Retrieves the "name" claim from the current authenticated Jwt.
     *
     * @return Optional containing the name if present, empty otherwise.
     */
    public Optional<String> getCurrentUserFullName() {
        return getClaimAsString("name");
    }

    /**
     * Retrieves the "email" claim from the current authenticated Jwt.
     *
     * @return Optional containing the email if present, empty otherwise.
     */
    public Optional<String> getCurrentUserEmail() {
        return getClaimAsString("email");
    }

    /**
     * Retrieves a claim as a string from the current authenticated Jwt.
     *
     * @param claim the name of the claim
     * @return Optional containing the claim value if present, empty otherwise.
     */
    public Optional<String> getClaimAsString(String claim) {
        return getCurrentJwt().map(jwt -> jwt.getClaimAsString(claim));
    }

    /**
     * Checks if the current authenticated user has the ADMIN role.
     *
     * @return true if the user has the ADMIN role, false otherwise.
     */
    public boolean isAdmin() {
        return getCurrentJwt()
                .map(jwt -> {
                    java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                    if (realmAccess != null) {
                        java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
                        return roles != null && roles.contains("ADMIN");
                    }
                    return false;
                }).orElse(false);
    }
}
