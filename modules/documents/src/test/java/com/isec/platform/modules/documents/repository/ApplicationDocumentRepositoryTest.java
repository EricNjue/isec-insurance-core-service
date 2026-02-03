package com.isec.platform.modules.documents.repository;

import com.isec.platform.modules.documents.domain.ApplicationDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@org.springframework.boot.test.context.SpringBootTest(classes = ApplicationDocumentRepositoryTest.TestConfig.class, properties = "spring.liquibase.enabled=false")
@org.springframework.transaction.annotation.Transactional
public class ApplicationDocumentRepositoryTest {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.boot.autoconfigure.SpringBootApplication
    @org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.isec.platform.modules.documents.domain")
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.isec.platform.modules.documents.repository")
    static class TestConfig {}

    @Autowired
    private ApplicationDocumentRepository repository;

    @Test
    void testSaveApplicationDocument() {
        ApplicationDocument doc = ApplicationDocument.builder()
                .applicationId(1L)
                .documentType("LOGBOOK")
                .s3Key("apps/1/logbook.pdf")
                .lastPresignedUrl("http://example.com/get")
                .build();

        ApplicationDocument saved = repository.save(doc);

        assertNotNull(saved.getId());
        assertEquals("apps/1/logbook.pdf", saved.getS3Key());
    }
}
