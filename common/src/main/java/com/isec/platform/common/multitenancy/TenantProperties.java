package com.isec.platform.common.multitenancy;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "tenant")
public class TenantProperties {
    private List<String> publicPatterns = new ArrayList<>();
}
