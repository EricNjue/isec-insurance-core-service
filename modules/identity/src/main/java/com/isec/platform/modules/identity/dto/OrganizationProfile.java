package com.isec.platform.modules.identity.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class OrganizationProfile {
    private String name;
    private String id;
    private List<String> agencyCodes;
}
