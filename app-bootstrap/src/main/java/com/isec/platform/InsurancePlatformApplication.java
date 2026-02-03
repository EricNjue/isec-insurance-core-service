package com.isec.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.isec.platform")
@EntityScan(basePackages = "com.isec.platform.modules")
@EnableJpaRepositories(basePackages = "com.isec.platform.modules")
@EnableAspectJAutoProxy
public class InsurancePlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(InsurancePlatformApplication.class, args);
    }
}
