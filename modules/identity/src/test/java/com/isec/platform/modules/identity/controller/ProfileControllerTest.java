package com.isec.platform.modules.identity.controller;

import com.isec.platform.modules.identity.dto.OrganizationProfile;
import com.isec.platform.modules.identity.dto.UserProfile;
import com.isec.platform.modules.identity.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController profileController;

    @Test
    void getProfile_delegatesToService_andReturnsProfile() {
        UserProfile mockProfile = UserProfile.builder()
                .userId("user-123")
                .email("user@example.com")
                .fullName("User Name")
                .roles(List.of("USER"))
                .organizations(List.of(
                        OrganizationProfile.builder().name("isecinsurance").id("id-1").agencyCodes(List.of("A1")).build(),
                        OrganizationProfile.builder().name("icealion").id("id-2").agencyCodes(List.of("B1")).build()
                ))
                .build();

        when(profileService.getUserProfile()).thenReturn(mockProfile);

        ResponseEntity<UserProfile> response = profileController.getProfile();

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("user-123", response.getBody().getUserId());
        assertEquals(2, response.getBody().getOrganizations().size());
    }
}
