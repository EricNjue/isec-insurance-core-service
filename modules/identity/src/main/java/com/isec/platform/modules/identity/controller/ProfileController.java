package com.isec.platform.modules.identity.controller;

import com.isec.platform.common.security.SecurityContextService;
import com.isec.platform.modules.customers.domain.Customer;
import com.isec.platform.modules.customers.service.CustomerService;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final CustomerService customerService;
    private final SecurityContextService securityContextService;

    @GetMapping
    public ResponseEntity<UserProfile> getProfile() {
        String userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));

        log.debug("Fetching profile for user: {}", userId);
        List<String> roles = List.of();
        Jwt jwt = securityContextService.getCurrentJwt().get(); // userId check ensures jwt exists
        if (jwt.getClaimAsMap("realm_access") != null) {
            roles = (List<String>) jwt.getClaimAsMap("realm_access").get("roles");
        }

        Optional<Customer> customer = customerService.getCustomerByUserId(userId);

        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .email(customer.map(Customer::getEmail).orElse(securityContextService.getCurrentUserEmail().orElse("N/A")))
                .fullName(customer.map(Customer::getFullName).orElse(securityContextService.getCurrentUserFullName().orElse("N/A")))
                .phoneNumber(customer.map(Customer::getPhoneNumber).orElse(null))
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
        private String phoneNumber;
        private List<String> roles;
    }
}
