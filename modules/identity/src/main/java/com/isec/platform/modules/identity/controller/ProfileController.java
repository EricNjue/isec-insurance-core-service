package com.isec.platform.modules.identity.controller;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    @GetMapping
    public ResponseEntity<UserProfile> getProfile(@AuthenticationPrincipal Jwt jwt) {
        log.debug("Fetching profile for user: {}", jwt.getSubject());
        List<String> roles = List.of();
        if (jwt.getClaimAsMap("realm_access") != null) {
            roles = (List<String>) jwt.getClaimAsMap("realm_access").get("roles");
        }

        UserProfile profile = UserProfile.builder()
                .userId(jwt.getSubject())
                .email(jwt.getClaimAsString("email"))
                .fullName(jwt.getClaimAsString("name"))
                .roles(roles)
                .build();
        return ResponseEntity.ok(profile);
    }

    @Data
    @Builder
    public static class UserProfile {
        private String userId;
        private String email;
        private String fullName;
        private List<String> roles;
    }
}
