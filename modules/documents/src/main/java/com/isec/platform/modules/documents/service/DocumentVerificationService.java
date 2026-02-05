package com.isec.platform.modules.documents.service;

import com.isec.platform.modules.documents.domain.ValuationLetter;
import com.isec.platform.modules.documents.repository.ValuationLetterRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentVerificationService {

    private final ValuationLetterRepository letterRepository;
    private final PdfSecurityService pdfSecurityService;

    @Value("${documents.valuation-letter.validity-days:14}")
    private int validityDays;

    @Transactional
    public VerificationResult verifyByUuid(UUID uuid) {
        log.info("Verifying document by UUID: {}", uuid);
        return letterRepository.findByDocumentUuid(uuid)
                .map(this::processVerification)
                .orElse(VerificationResult.builder()
                        .status("NOT_FOUND")
                        .message("Document not found in our records")
                        .build());
    }

    @Transactional
    public VerificationResult verifyByPdfContent(byte[] content, String fileName) {
        log.info("Verifying document by PDF content. File: {}", fileName);
        try {
            String hash = pdfSecurityService.calculateHash(content);
            Map<String, String> metadata = pdfSecurityService.extractMetadata(content);

            String docIdStr = metadata.get("documentId");
            if (docIdStr == null) {
                log.warn("Verification Failed: No metadata found in PDF {}", fileName);
                return VerificationResult.builder()
                        .status("MODIFIED")
                        .message("No document metadata found in the PDF")
                        .build();
            }

            UUID uuid = UUID.fromString(docIdStr);
            return letterRepository.findByDocumentUuid(uuid)
                    .map(letter -> {
                        if (letter.getDocumentHash().equals(hash)) {
                            VerificationResult result = processVerification(letter);
                            return result.toBuilder()
                                    .message("Cryptographic hash matches record")
                                    .build();
                        } else {
                            log.error("Verification ALERT: documentId={} hash MISMATCH. Possible tampering detected!", uuid);
                            return VerificationResult.builder()
                                    .documentId(letter.getDocumentUuid().toString())
                                    .status("MODIFIED")
                                    .message("PDF content does not match the original hash")
                                    .build();
                        }
                    })
                    .orElseGet(() -> {
                        log.warn("Verification Failed: Metadata docId={} found in PDF but not in DB", uuid);
                        return VerificationResult.builder()
                                .status("NOT_FOUND")
                                .message("Document metadata exists but record not found")
                                .build();
                    });
        } catch (IllegalArgumentException e) {
            log.error("Verification Error: Invalid UUID in PDF {}", fileName, e);
            return VerificationResult.builder()
                    .status("MODIFIED")
                    .message("Invalid document ID in PDF metadata")
                    .build();
        }
    }

    private VerificationResult processVerification(ValuationLetter letter) {
        String status = resolveStatus(letter);
        if ("EXPIRED".equals(status) && letter.getStatus() != ValuationLetter.ValuationLetterStatus.EXPIRED) {
            letter.setStatus(ValuationLetter.ValuationLetterStatus.EXPIRED);
            letterRepository.save(letter);
            log.info("Document verification: Updated documentId={} status to EXPIRED in database", letter.getDocumentUuid());
        }

        return VerificationResult.builder()
                .documentId(letter.getDocumentUuid().toString())
                .status(status)
                .issuedAt(letter.getGeneratedAt() != null ? letter.getGeneratedAt().toString() : null)
                .documentType(letter.getDocumentType())
                .build();
    }

    private String resolveStatus(ValuationLetter letter) {
        if (letter.getStatus() == ValuationLetter.ValuationLetterStatus.REVOKED) {
            return "REVOKED";
        }

        if (letter.getGeneratedAt() != null &&
                letter.getGeneratedAt().isBefore(LocalDateTime.now().minusDays(validityDays))) {
            return "EXPIRED";
        }

        if (letter.getStatus() == ValuationLetter.ValuationLetterStatus.EXPIRED) {
            return "EXPIRED";
        }

        return "VALID";
    }

    @Data
    @Builder(toBuilder = true)
    public static class VerificationResult {
        private String documentId;
        private String status;
        private String issuedAt;
        private String documentType;
        private String message;
    }
}
