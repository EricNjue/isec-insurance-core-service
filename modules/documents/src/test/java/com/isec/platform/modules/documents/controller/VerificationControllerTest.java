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

@WebMvcTest(controllers = VerificationController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ContextConfiguration(classes = {VerificationController.class, VerificationControllerTest.TestConfig.class})
public class VerificationControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration(exclude = {
            org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
            org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
    })
    static class TestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentVerificationService verificationService;

    @Test
    void testVerifyDocument_Valid() throws Exception {
        UUID uuid = UUID.randomUUID();
        VerificationResult result = VerificationResult.builder()
                .documentId(uuid.toString())
                .status("VALID")
                .documentType("VALUATION_LETTER")
                .build();

        when(verificationService.verifyByUuid(uuid)).thenReturn(result);

        mockMvc.perform(get("/verify/doc/" + uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.documentId").value(uuid.toString()));
    }

    @Test
    void testVerifyDocument_Revoked() throws Exception {
        UUID uuid = UUID.randomUUID();
        VerificationResult result = VerificationResult.builder()
                .documentId(uuid.toString())
                .status("REVOKED")
                .build();

        when(verificationService.verifyByUuid(uuid)).thenReturn(result);

        mockMvc.perform(get("/verify/doc/" + uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }

    @Test
    void testVerifyDocument_NotFound() throws Exception {
        UUID uuid = UUID.randomUUID();
        VerificationResult result = VerificationResult.builder()
                .status("NOT_FOUND")
                .message("Document not found in our records")
                .build();

        when(verificationService.verifyByUuid(any())).thenReturn(result);

        mockMvc.perform(get("/verify/doc/" + uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void testVerifyUpload_Valid() throws Exception {
        UUID uuid = UUID.randomUUID();
        String issuedAt = "2026-02-05T13:27:45.312";
        VerificationResult result = VerificationResult.builder()
                .documentId(uuid.toString())
                .status("VALID")
                .issuedAt(issuedAt)
                .documentType("VALUATION_LETTER")
                .message("Cryptographic hash matches record")
                .build();

        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy content".getBytes());

        when(verificationService.verifyByPdfContent(any(), eq("test.pdf"))).thenReturn(result);

        mockMvc.perform(multipart("/verify/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.documentId").value(uuid.toString()))
                .andExpect(jsonPath("$.issuedAt").value(issuedAt))
                .andExpect(jsonPath("$.documentType").value("VALUATION_LETTER"))
                .andExpect(jsonPath("$.message").value("Cryptographic hash matches record"));
    }
}
