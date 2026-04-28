package com.isec.platform.modules.integrations.premium.sanlam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "integrations.premium.sanlam")
public class SanlamPremiumCalculationProperties {
    private String baseUrl;
    private String calculatePremiumPath = "/rates/motor/calculate-premium";
    private Duration timeout = Duration.ofSeconds(5);
    private RetryProperties retry = new RetryProperties();

    @Data
    public static class RetryProperties {
        private boolean enabled = true;
        private int maxAttempts = 3;
        private Duration backoff = Duration.ofMillis(500);
    }
}
