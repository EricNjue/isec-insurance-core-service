package com.isec.platform.modules.integrations.common.enums;

import lombok.Getter;

@Getter
public enum ReferenceCategory {
    POLICY_TYPE("Policy Type"),
    COVER_TYPE("Cover Type"),
    BODY_TYPE("Body Type"),
    VEHICLE_USAGE("Vehicle Usage"),
    VEHICLE_MAKE("Vehicle Make"),
    VEHICLE_MODEL("Vehicle Model"),
    CITY("City"),
    BRANCH("Branch");

    private final String defaultLabel;

    ReferenceCategory(String defaultLabel) {
        this.defaultLabel = defaultLabel;
    }
}
