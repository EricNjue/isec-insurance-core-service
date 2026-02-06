package com.isec.platform.modules.notifications.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "africastalking")
public class AfricasTalkingProperties {
    private String username;
    private String apiKey;
    private String from;
    private String baseUrl;
}
