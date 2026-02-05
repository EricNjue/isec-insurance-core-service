package com.isec.platform.modules.documents.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QrCodeServiceTest {

    private final QrCodeService qrCodeService = new QrCodeService();

    @Test
    void testGenerateQrCode() {
        String text = "https://verify.example.com/doc/123";
        byte[] qrCode = qrCodeService.generateQrCode(text, 200, 200);
        
        assertNotNull(qrCode);
        assertTrue(qrCode.length > 0);
        // PNG header check
        assertEquals((byte) 0x89, qrCode[0]);
        assertEquals((byte) 'P', qrCode[1]);
        assertEquals((byte) 'N', qrCode[2]);
        assertEquals((byte) 'G', qrCode[3]);
    }
}
