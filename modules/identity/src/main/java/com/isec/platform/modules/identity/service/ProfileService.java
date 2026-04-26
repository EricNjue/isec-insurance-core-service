package com.isec.platform.modules.identity.service;

import com.isec.platform.common.security.SecurityContextService;
import com.isec.platform.modules.customers.domain.Customer;
import com.isec.platform.modules.customers.service.CustomerService;
import com.isec.platform.modules.identity.dto.OrganizationProfile;
import com.isec.platform.modules.identity.dto.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final CustomerService customerService;
    private final SecurityContextService securityContextService;

    public Mono<UserProfile> getUserProfile() {
        return securityContextService.getCurrentUserId()
                .switchIfEmpty(Mono.error(new IllegalStateException("User not authenticated")))
                .flatMap(userId -> {
                    log.debug("Fetching profile for user: {}", userId);
                    return securityContextService.getCurrentJwt()
                            .switchIfEmpty(Mono.error(new IllegalStateException("JWT not found in security context")))
                            .flatMap(jwt -> {
                                List<String> roles = extractRoles(jwt);
                                List<OrganizationProfile> organizations = extractOrganizations(jwt);

                                return customerService.getCustomerByUserId(userId)
                                        .map(Optional::of)
                                        .defaultIfEmpty(Optional.empty())
                                        .flatMap(customer -> {
                                            Mono<String> emailMono = customer.map(Customer::getEmail)
                                                    .map(Mono::just)
                                                    .orElseGet(() -> securityContextService.getCurrentUserEmail().defaultIfEmpty("N/A"));

                                            Mono<String> fullNameMono = customer.map(Customer::getFullName)
                                                    .map(Mono::just)
                                                    .orElseGet(() -> securityContextService.getCurrentUserFullName().defaultIfEmpty("N/A"));

                                            return Mono.zip(emailMono, fullNameMono)
                                                    .map(tuple -> UserProfile.builder()
                                                            .userId(userId)
                                                            .email(tuple.getT1())
                                                            .fullName(tuple.getT2())
                                                            .phoneNumber(customer.map(Customer::getPhoneNumber).orElse(null))
                                                            .roles(roles)
                                                            .organizations(organizations)
                                                            .build());
                                        });
                            });
                });
    }

    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            return (List<String>) roles;
        }
        return List.of();
    }

    private List<OrganizationProfile> extractOrganizations(Jwt jwt) {
        Map<String, Object> orgsMap = jwt.getClaimAsMap("organization");
        if (orgsMap == null) {
            return List.of();
        }
        return orgsMap.entrySet().stream()
                .map(entry -> {
                    String name = entry.getKey();
                    if (entry.getValue() instanceof Map<?, ?> details) {
                        String id = (String) details.get("id");
                        List<String> agencyCodes = (List<String>) details.get("agencyCode");
                        return OrganizationProfile.builder()
                                .name(name)
                                .id(id)
                                .agencyCodes(agencyCodes)
                                .build();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
