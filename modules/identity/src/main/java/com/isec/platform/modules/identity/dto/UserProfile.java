package com.isec.platform.modules.identity.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserProfile {
    private String userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private List<String> roles;
    private List<OrganizationProfile> organizations;
}
