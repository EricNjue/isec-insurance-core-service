package com.isec.platform.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "security.cors")
public class SecurityProperties {
    private List<String> allowedOrigins = new ArrayList<>();
}
