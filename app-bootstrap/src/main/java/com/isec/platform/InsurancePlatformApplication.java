package com.isec.platform;

import com.isec.platform.reactive.infra.config.ReactiveInfraConfig;
import com.isec.platform.reactive.infra.outbox.OutboxRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.isec.platform")
@EnableR2dbcRepositories(basePackages = {
    "com.isec.platform.reactive.infra",
    "com.isec.platform.modules.customers.repository",
    "com.isec.platform.modules.applications.repository",
    "com.isec.platform.modules.rating.repository",
    "com.isec.platform.modules.documents.repository",
    "com.isec.platform.modules.payments.repository",
    "com.isec.platform.modules.policies.repository",
    "com.isec.platform.modules.certificates.repository",
    "com.isec.platform.modules.notifications.repository",
    "com.isec.platform.modules.audit.repository",
    "com.isec.platform.modules.vehicles.repository",
    "com.isec.platform.modules.integrations.mpesa.repository",
    "com.isec.platform.modules.integrations.registry.repository"
})
@EnableRedisRepositories(basePackages = "com.isec.platform.modules.rating.repository")
@EnableAspectJAutoProxy
@EnableScheduling
@EnableAsync
@Import(ReactiveInfraConfig.class)
public class InsurancePlatformApplication {
    static {
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
    }

    public static void main(String[] args) {
        SpringApplication.run(InsurancePlatformApplication.class, args);
    }
}
