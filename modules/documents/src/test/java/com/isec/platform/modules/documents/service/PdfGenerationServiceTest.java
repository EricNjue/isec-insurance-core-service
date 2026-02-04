package com.isec.platform.modules.documents.service;

import com.isec.platform.modules.documents.domain.AuthorizedValuer;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PdfGenerationServiceTest {

    private final PdfGenerationService pdfGenerationService;

    PdfGenerationServiceTest() {
        pdfGenerationService = new PdfGenerationService(new DefaultResourceLoader());
        ReflectionTestUtils.setField(pdfGenerationService, "logoPath", "classpath:/branding/isec_logo.jpg");
    }

    @Test
    void testGenerateValuationLetter() {
        Map<String, Object> data = new HashMap<>();
        data.put("insuredName", "John Doe");
        data.put("policyNumber", "POL-123");
        data.put("registrationNumber", "KAA 001Z");

        AuthorizedValuer valuer = AuthorizedValuer.builder()
                .companyName("Test Valuer")
                .locations("Nairobi")
                .phoneNumbers("0700000000")
                .build();

        byte[] pdf = pdfGenerationService.generateValuationLetter(data, Collections.singletonList(valuer));

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        // Basic PDF header check
        String pdfString = new String(pdf, 0, Math.min(pdf.length, 10));
        assertTrue(pdfString.startsWith("%PDF-"));
    }
}
