package com.isec.platform.modules.certificates.metrics;

import com.isec.platform.modules.certificates.domain.canonical.ProviderType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ProviderMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public ProviderMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordSuccess(ProviderType providerType, Duration duration) {
        Timer.builder("certificates.provider.call")
                .tag("provider", providerType.name())
                .tag("outcome", "success")
                .register(meterRegistry)
                .record(duration);

        Counter.builder("certificates.provider.success")
                .tag("provider", providerType.name())
                .register(meterRegistry)
                .increment();
    }

    public void recordFailure(ProviderType providerType, Duration duration) {
        Timer.builder("certificates.provider.call")
                .tag("provider", providerType.name())
                .tag("outcome", "failure")
                .register(meterRegistry)
                .record(duration);

        Counter.builder("certificates.provider.failure")
                .tag("provider", providerType.name())
                .register(meterRegistry)
                .increment();
    }
}
