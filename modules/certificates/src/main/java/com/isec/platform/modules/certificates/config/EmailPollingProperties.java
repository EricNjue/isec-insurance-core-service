package com.isec.platform.modules.certificates.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "email.polling")
public class EmailPollingProperties {
    /**
     * Polling interval in IDLE mode.
     */
    private int idleIntervalSeconds = 60;

    /**
     * Polling interval in ACTIVE mode.
     */
    private int activeIntervalSeconds = 20;

    /**
     * How long we stay aggressive after activity (seconds).
     */
    private int activeModeDurationSeconds = 300;

    /**
     * Max emails to process per polling cycle.
     */
    private int maxBatchSize = 10;

    /**
     * Jitter percentage (e.g. 15 means ±15%).
     */
    private int jitterPercentage = 15;

    /**
     * Whether to enable locking to prevent overlapping runs.
     */
    private boolean lockEnabled = true;
}
