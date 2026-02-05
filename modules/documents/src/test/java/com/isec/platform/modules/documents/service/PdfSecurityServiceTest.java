package com.isec.platform.modules.documents.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PdfSecurityServiceTest {

    private final PdfSecurityService securityService = new PdfSecurityService();

    @Test
    void testCalculateHash() {
        byte[] content = "Hello World".getBytes();
        String hash1 = securityService.calculateHash(content);
        String hash2 = securityService.calculateHash(content);
        
        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
        
        byte[] modifiedContent = "Hello World!".getBytes();
        String hash3 = securityService.calculateHash(modifiedContent);
        assertNotEquals(hash1, hash3);
    }
}
