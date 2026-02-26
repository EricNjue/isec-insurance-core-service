package com.isec.platform.modules.certificates.registry;

import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.certificates.adapters.CertificateProviderAdapter;
import com.isec.platform.modules.certificates.domain.canonical.CertificateRequest;
import com.isec.platform.modules.certificates.domain.canonical.CertificateResponse;
import com.isec.platform.modules.certificates.domain.canonical.ProviderType;
import com.isec.platform.modules.certificates.domain.entity.InsuranceProviderEntity;
import com.isec.platform.modules.certificates.domain.entity.TenantProviderMappingEntity;
import com.isec.platform.modules.certificates.repository.InsuranceProviderRepository;
import com.isec.platform.modules.certificates.repository.TenantProviderMappingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Sql(statements = {
        "create table insurance_provider (" +
                "id uuid not null," +
                "provider_code varchar(50) not null," +
                "base_url varchar(500) not null," +
                "auth_type varchar(50)," +
                "timeout_ms integer," +
                "retry_count integer," +
                "active boolean not null," +
                "created_at timestamp," +
                "updated_at timestamp," +
                "version bigint," +
                "primary key (id)," +
                "unique (provider_code)" +
                ")",
        "create table tenant_provider_mapping (" +
                "id uuid not null," +
                "tenant_id varchar(100) not null," +
                "provider_code varchar(50) not null," +
                "active boolean not null," +
                "created_at timestamp," +
                "updated_at timestamp," +
                "version bigint," +
                "primary key (id)" +
                ")"
})
@ContextConfiguration(classes = {
        CertificateProviderRegistryIntegrationTest.TestApp.class,
        CertificateProviderRegistryIntegrationTest.JpaConfig.class
})
@Import(CertificateProviderRegistry.class)
class CertificateProviderRegistryIntegrationTest {

    @Autowired
    private TenantProviderMappingRepository tenantProviderMappingRepository;

    @Autowired
    private InsuranceProviderRepository insuranceProviderRepository;

    @Autowired
    private CertificateProviderRegistry registry;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("APA");

        insuranceProviderRepository.save(InsuranceProviderEntity.builder()
                .providerCode(ProviderType.APA)
                .baseUrl("https://api.apa.example")
                .active(true)
                .build());

        tenantProviderMappingRepository.save(TenantProviderMappingEntity.builder()
                .providerCode(ProviderType.APA)
                .active(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void resolvesProviderFromTenantMapping() {
        CertificateProviderAdapter adapter = registry.resolveProvider("APA", null);
        assertEquals(ProviderType.APA, adapter.providerType());
    }

    @TestConfiguration
    @EnableJpaRepositories(basePackages = "com.isec.platform.modules.certificates.repository")
    static class JpaConfig {
        @Bean
        List<CertificateProviderAdapter> adapters() {
            return List.of(new StubAdapter());
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            InsuranceProviderEntity.class,
            TenantProviderMappingEntity.class
    })
    static class TestApp {
    }

    static class StubAdapter implements CertificateProviderAdapter {
        @Override
        public ProviderType providerType() {
            return ProviderType.APA;
        }

        @Override
        public CertificateResponse issueCertificate(CertificateRequest request) {
            return null;
        }

        @Override
        public CertificateResponse checkStatus(String externalReference) {
            return null;
        }
    }
}
