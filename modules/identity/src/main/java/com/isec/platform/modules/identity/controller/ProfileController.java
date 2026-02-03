package com.isec.platform.modules.identity.controller;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
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
public class ProfileController {

    @GetMapping
    public ResponseEntity<UserProfile> getProfile(@AuthenticationPrincipal Jwt jwt) {
        UserProfile profile = UserProfile.builder()
                .userId(jwt.getSubject())
                .email(jwt.getClaimAsString("email"))
                .roles(jwt.getClaimAsStringList("realm_access.roles"))
                .build();
        return ResponseEntity.ok(profile);
    }

    @Data
    @Builder
    public static class UserProfile {
        private String userId;
        private String email;
        private List<String> roles;
    }
}
