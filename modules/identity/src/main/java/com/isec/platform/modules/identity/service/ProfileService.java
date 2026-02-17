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

    public UserProfile getUserProfile() {
        String userId = securityContextService.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));

        log.debug("Fetching profile for user: {}", userId);
        
        Jwt jwt = securityContextService.getCurrentJwt()
                .orElseThrow(() -> new IllegalStateException("JWT not found in security context"));

        List<String> roles = extractRoles(jwt);
        List<OrganizationProfile> organizations = extractOrganizations(jwt);

        Optional<Customer> customer = customerService.getCustomerByUserId(userId);

        return UserProfile.builder()
                .userId(userId)
                .email(customer.map(Customer::getEmail).orElse(securityContextService.getCurrentUserEmail().orElse("N/A")))
                .fullName(customer.map(Customer::getFullName).orElse(securityContextService.getCurrentUserFullName().orElse("N/A")))
                .phoneNumber(customer.map(Customer::getPhoneNumber).orElse(null))
                .roles(roles)
                .organizations(organizations)
                .build();
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
