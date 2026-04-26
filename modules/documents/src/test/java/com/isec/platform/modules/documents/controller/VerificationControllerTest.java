package com.isec.platform.modules.documents.controller;

import com.isec.platform.modules.documents.service.DocumentVerificationService;
import com.isec.platform.modules.documents.service.DocumentVerificationService.VerificationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

@org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest(controllers = VerificationController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration.class
})
@org.springframework.test.context.ContextConfiguration(classes = {VerificationController.class, VerificationControllerTest.TestConfig.class})
public class VerificationControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestConfig {}

    @Autowired
    private org.springframework.test.web.reactive.server.WebTestClient webTestClient;

    @MockBean
    private DocumentVerificationService verificationService;

    @Test
    void testVerifyDocument_Valid() {
        UUID uuid = UUID.randomUUID();
        VerificationResult result = VerificationResult.builder()
                .documentId(uuid.toString())
                .status("VALID")
                .documentType("VALUATION_LETTER")
                .build();

        when(verificationService.verifyByUuid(uuid)).thenReturn(Mono.just(result));

        webTestClient.get()
                .uri("/verify/doc/" + uuid)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("VALID")
                .jsonPath("$.documentId").isEqualTo(uuid.toString());
    }

    @Test
    void testVerifyDocument_Revoked() {
        UUID uuid = UUID.randomUUID();
        VerificationResult result = VerificationResult.builder()
                .documentId(uuid.toString())
                .status("REVOKED")
                .build();

        when(verificationService.verifyByUuid(uuid)).thenReturn(Mono.just(result));

        webTestClient.get()
                .uri("/verify/doc/" + uuid)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("REVOKED");
    }

    @Test
    void testVerifyDocument_NotFound() {
        UUID uuid = UUID.randomUUID();
        VerificationResult result = VerificationResult.builder()
                .status("NOT_FOUND")
                .message("Document not found in our records")
                .build();

        when(verificationService.verifyByUuid(any())).thenReturn(Mono.just(result));

        webTestClient.get()
                .uri("/verify/doc/" + uuid)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("NOT_FOUND");
    }

    @Test
    void testVerifyUpload_Valid() {
        UUID uuid = UUID.randomUUID();
        String issuedAt = "2026-02-05T13:27:45.312";
        VerificationResult result = VerificationResult.builder()
                .documentId(uuid.toString())
                .status("VALID")
                .issuedAt(issuedAt)
                .documentType("VALUATION_LETTER")
                .message("Cryptographic hash matches record")
                .build();

        byte[] content = "dummy content".getBytes();
        org.springframework.http.client.MultipartBodyBuilder builder = new org.springframework.http.client.MultipartBodyBuilder();
        builder.part("file", content)
                .filename("test.pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF);

        when(verificationService.verifyByPdfContent(any(), eq("test.pdf"))).thenReturn(Mono.just(result));

        webTestClient.post()
                .uri("/verify/upload")
                .body(org.springframework.web.reactive.function.BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("VALID")
                .jsonPath("$.documentId").isEqualTo(uuid.toString())
                .jsonPath("$.issuedAt").isEqualTo(issuedAt)
                .jsonPath("$.documentType").isEqualTo("VALUATION_LETTER")
                .jsonPath("$.message").isEqualTo("Cryptographic hash matches record");
    }
}
