package com.isec.platform.modules.identity.service;

import com.isec.platform.common.security.SecurityContextService;
import com.isec.platform.modules.customers.domain.Customer;
import com.isec.platform.modules.customers.service.CustomerService;
import com.isec.platform.modules.identity.dto.OrganizationProfile;
import com.isec.platform.modules.identity.dto.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void getUserProfile_extractsRolesAndOrganizations_andFallsBackToJwtClaims() {
        String userId = "user-123";
        when(securityContextService.getCurrentUserId()).thenReturn(Mono.just(userId));
        when(securityContextService.getCurrentJwt()).thenReturn(Mono.just(jwt));
        when(securityContextService.getCurrentUserEmail()).thenReturn(Mono.just("user@example.com"));
        when(securityContextService.getCurrentUserFullName()).thenReturn(Mono.just("User Name"));
        when(customerService.getCustomerByUserId(userId)).thenReturn(Mono.empty());

        Map<String, Object> realmAccess = Map.of("roles", List.of("USER", "ADMIN"));
        when(jwt.getClaimAsMap("realm_access")).thenReturn(realmAccess);

        Map<String, Object> organizations = Map.of(
                "isecinsurance", Map.of(
                        "id", "8a67ccf8-6c58-4858-b476-0cea4db711fc",
                        "agencyCode", List.of("AG03001938 - AVON INSURANCE AGENCY")
                ),
                "icealion", Map.of(
                        "id", "3409773d-eead-46f8-925f-8e84194daf2c",
                        "agencyCode", List.of("ICEA2034AQ23-09232")
                )
        );
        when(jwt.getClaimAsMap("organization")).thenReturn(organizations);

        Mono<UserProfile> profileMono = profileService.getUserProfile();

        StepVerifier.create(profileMono)
                .assertNext(profile -> {
                    assertNotNull(profile);
                    assertEquals(userId, profile.getUserId());
                    assertEquals("user@example.com", profile.getEmail());
                    assertEquals("User Name", profile.getFullName());
                    assertEquals(List.of("USER", "ADMIN"), profile.getRoles());
                    assertEquals(2, profile.getOrganizations().size());

                    OrganizationProfile isec = profile.getOrganizations().stream()
                            .filter(o -> o.getName().equals("isecinsurance"))
                            .findFirst().orElseThrow();
                    assertEquals("8a67ccf8-6c58-4858-b476-0cea4db711fc", isec.getId());
                    assertEquals(List.of("AG03001938 - AVON INSURANCE AGENCY"), isec.getAgencyCodes());
                })
                .verifyComplete();
    }

    @Test
    void getUserProfile_usesCustomerDetails_whenAvailable() {
        String userId = "user-456";
        when(securityContextService.getCurrentUserId()).thenReturn(Mono.just(userId));
        when(securityContextService.getCurrentJwt()).thenReturn(Mono.just(jwt));

        Customer customer = new Customer();
        customer.setEmail("cust@example.com");
        customer.setFullName("Customer Name");
        customer.setPhoneNumber("0700000000");
        when(customerService.getCustomerByUserId(userId)).thenReturn(Mono.just(customer));

        when(jwt.getClaimAsMap("realm_access")).thenReturn(null);
        when(jwt.getClaimAsMap("organization")).thenReturn(null);

        Mono<UserProfile> profileMono = profileService.getUserProfile();

        StepVerifier.create(profileMono)
                .assertNext(profile -> {
                    assertEquals("cust@example.com", profile.getEmail());
                    assertEquals("Customer Name", profile.getFullName());
                    assertEquals("0700000000", profile.getPhoneNumber());
                })
                .verifyComplete();
    }
}
