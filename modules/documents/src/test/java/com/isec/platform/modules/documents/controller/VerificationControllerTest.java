package com.isec.platform.modules.documents.controller;

import com.isec.platform.modules.documents.domain.ValuationLetter;
import com.isec.platform.modules.documents.repository.ValuationLetterRepository;
import com.isec.platform.modules.documents.service.PdfSecurityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
    private ValuationLetterRepository letterRepository;

    @MockBean
    private PdfSecurityService pdfSecurityService;

    @Test
    void testVerifyDocument_Valid() throws Exception {
        UUID uuid = UUID.randomUUID();
        ValuationLetter letter = ValuationLetter.builder()
                .documentUuid(uuid)
                .status(ValuationLetter.ValuationLetterStatus.ACTIVE)
                .generatedAt(LocalDateTime.now())
                .documentType("VALUATION_LETTER")
                .build();

        when(letterRepository.findByDocumentUuid(uuid)).thenReturn(Optional.of(letter));

        mockMvc.perform(get("/verify/doc/" + uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.documentId").value(uuid.toString()));
    }

    @Test
    void testVerifyDocument_Revoked() throws Exception {
        UUID uuid = UUID.randomUUID();
        ValuationLetter letter = ValuationLetter.builder()
                .documentUuid(uuid)
                .status(ValuationLetter.ValuationLetterStatus.REVOKED)
                .generatedAt(LocalDateTime.now())
                .documentType("VALUATION_LETTER")
                .build();

        when(letterRepository.findByDocumentUuid(uuid)).thenReturn(Optional.of(letter));

        mockMvc.perform(get("/verify/doc/" + uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }

    @Test
    void testVerifyDocument_NotFound() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(letterRepository.findByDocumentUuid(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/verify/doc/" + uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void testVerifyUpload_Valid() throws Exception {
        UUID uuid = UUID.randomUUID();
        String hash = "matching-hash";
        LocalDateTime now = LocalDateTime.now();
        ValuationLetter letter = ValuationLetter.builder()
                .documentUuid(uuid)
                .status(ValuationLetter.ValuationLetterStatus.ACTIVE)
                .documentHash(hash)
                .generatedAt(now)
                .documentType("VALUATION_LETTER")
                .build();

        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy content".getBytes());

        when(pdfSecurityService.calculateHash(any())).thenReturn(hash);
        when(pdfSecurityService.extractMetadata(any())).thenReturn(Map.of("documentId", uuid.toString()));
        when(letterRepository.findByDocumentUuid(uuid)).thenReturn(Optional.of(letter));

        mockMvc.perform(multipart("/verify/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.documentId").value(uuid.toString()))
                .andExpect(jsonPath("$.issuedAt").value(now.toString()))
                .andExpect(jsonPath("$.documentType").value("VALUATION_LETTER"))
                .andExpect(jsonPath("$.message").value("Cryptographic hash matches record"));
    }
}
