package com.isec.platform.modules.documents.repository;

import com.isec.platform.modules.documents.domain.ApplicationDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
@org.springframework.test.context.ContextConfiguration(classes = ApplicationDocumentRepositoryTest.TestConfig.class)
public class ApplicationDocumentRepositoryTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    static class TestConfig {}

    @Autowired
    private ApplicationDocumentRepository repository;

    @Autowired
    private org.springframework.r2dbc.core.DatabaseClient databaseClient;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        databaseClient.sql("CREATE TABLE IF NOT EXISTS application_documents (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "application_id BIGINT, " +
                "document_type VARCHAR(255), " +
                "s3_key VARCHAR(255), " +
                "last_presigned_url VARCHAR(1024), " +
                "url_expiry_at TIMESTAMP, " +
                "created_at TIMESTAMP" +
                ");").fetch().rowsUpdated().block();
        repository.deleteAll().onErrorResume(e -> reactor.core.publisher.Mono.empty()).block();
    }

    @Test
    void testSaveApplicationDocument() {
        ApplicationDocument doc = ApplicationDocument.builder()
                .applicationId(1L)
                .documentType("LOGBOOK")
                .s3Key("apps/1/logbook.pdf")
                .lastPresignedUrl("http://example.com/get")
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(doc)
                .as(StepVerifier::create)
                .consumeNextWith(saved -> {
                    assertNotNull(saved.getId());
                    assertEquals("apps/1/logbook.pdf", saved.getS3Key());
                })
                .verifyComplete();
    }
}
