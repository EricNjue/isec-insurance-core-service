package com.isec.platform.modules.documents.service;

import com.lowagie.text.pdf.PdfReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Service
@Slf4j
public class PdfSecurityService {

    public String calculateHash(byte[] content) {
        log.debug("Calculating SHA-256 hash for content (size={} bytes)", content.length);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            String hexHash = HexFormat.of().formatHex(hash);
            log.debug("Hash calculation successful: {}", hexHash);
            return hexHash;
        } catch (NoSuchAlgorithmException e) {
            log.error("CRITICAL: SHA-256 algorithm not found in JVM", e);
            throw new RuntimeException("Hashing failed", e);
        }
    }

    public Map<String, String> extractMetadata(byte[] pdfContent) {
        log.debug("Extracting metadata from PDF content (size={} bytes)", pdfContent.length);
        try {
            PdfReader reader = new PdfReader(pdfContent);
            Map<String, String> info = reader.getInfo();
            log.debug("Extracted {} metadata fields", info.size());
            return info;
        } catch (IOException e) {
            log.error("Failed to extract metadata from PDF content", e);
            throw new RuntimeException("Metadata extraction failed", e);
        }
    }
}
