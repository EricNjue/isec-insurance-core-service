package com.isec.platform.modules.documents.controller;

import com.isec.platform.modules.documents.domain.ValuationLetter;
import com.isec.platform.modules.documents.repository.ValuationLetterRepository;
import com.isec.platform.modules.documents.service.PdfSecurityService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/verify")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final ValuationLetterRepository letterRepository;
    private final PdfSecurityService pdfSecurityService;

    @GetMapping("/doc/{documentId}")
    public ResponseEntity<VerificationResponse> verifyDocument(@PathVariable String documentId) {
        log.info("Public Verification Request: Checking status for documentId={}", documentId);
        UUID uuid;
        try {
            uuid = UUID.fromString(documentId);
        } catch (IllegalArgumentException e) {
            log.warn("Public Verification Failed: Invalid UUID format for documentId={}", documentId);
            return ResponseEntity.badRequest().body(VerificationResponse.builder()
                    .status("NOT_FOUND")
                    .message("Invalid Document ID format")
                    .build());
        }

        return letterRepository.findByDocumentUuid(uuid)
                .map(letter -> {
                    String status = "VALID";
                    if (letter.getStatus() == ValuationLetter.ValuationLetterStatus.REVOKED) {
                        status = "REVOKED";
                    } else if (letter.getStatus() == ValuationLetter.ValuationLetterStatus.EXPIRED) {
                        status = "EXPIRED";
                    }

                    log.info("Public Verification Success: documentId={} status={} type={}", 
                            uuid, status, letter.getDocumentType());

                    return ResponseEntity.ok(VerificationResponse.builder()
                            .documentId(letter.getDocumentUuid().toString())
                            .status(status)
                            .issuedAt(letter.getGeneratedAt().toString())
                            .documentType(letter.getDocumentType())
                            .build());
                })
                .orElseGet(() -> {
                    log.warn("Public Verification Failed: Document not found for documentId={}", uuid);
                    return ResponseEntity.ok(VerificationResponse.builder()
                            .status("NOT_FOUND")
                            .message("Document not found in our records")
                            .build());
                });
    }

    @PostMapping("/upload")
    public ResponseEntity<VerificationResponse> verifyUploadedPdf(@RequestParam("file") MultipartFile file) {
        String fileName = file.getOriginalFilename();
        log.info("Cryptographic Verification Request: Uploaded file={}", fileName);
        try {
            byte[] content = file.getBytes();
            log.debug("Calculating hash for uploaded file (size={} bytes)", content.length);
            String hash = pdfSecurityService.calculateHash(content);
            java.util.Map<String, String> metadata = pdfSecurityService.extractMetadata(content);
            
            String docIdStr = metadata.get("documentId");
            if (docIdStr == null) {
                log.warn("Cryptographic Verification Failed: No metadata found in uploaded file={}", fileName);
                return ResponseEntity.ok(VerificationResponse.builder()
                        .status("MODIFIED")
                        .message("No document metadata found in the PDF")
                        .build());
            }

            UUID uuid = UUID.fromString(docIdStr);
            return letterRepository.findByDocumentUuid(uuid)
                    .map(letter -> {
                        if (letter.getDocumentHash().equals(hash)) {
                             String status = "VALID";
                            if (letter.getStatus() == ValuationLetter.ValuationLetterStatus.REVOKED) {
                                status = "REVOKED";
                            }
                            log.info("Cryptographic Verification Success: documentId={} matches record. status={}", uuid, status);
                            return ResponseEntity.ok(VerificationResponse.builder()
                                    .documentId(letter.getDocumentUuid().toString())
                                    .status(status)
                                    .issuedAt(letter.getGeneratedAt() != null ? letter.getGeneratedAt().toString() : null)
                                    .documentType(letter.getDocumentType())
                                    .message("Cryptographic hash matches record")
                                    .build());
                        } else {
                            log.error("Cryptographic Verification ALERT: documentId={} hash MISMATCH. Possible tampering detected!", uuid);
                            return ResponseEntity.ok(VerificationResponse.builder()
                                    .documentId(letter.getDocumentUuid().toString())
                                    .status("MODIFIED")
                                    .message("PDF content does not match the original hash")
                                    .build());
                        }
                    })
                    .orElseGet(() -> {
                        log.warn("Cryptographic Verification Failed: Metadata docId={} found in PDF but not in DB", uuid);
                        return ResponseEntity.ok(VerificationResponse.builder()
                                .status("NOT_FOUND")
                                .message("Document metadata exists but record not found")
                                .build());
                    });

        } catch (IOException | IllegalArgumentException e) {
            log.error("Cryptographic Verification Error: processing file={} failed", fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Builder
    @Data
    public static class VerificationResponse {
        private String documentId;
        private String status;
        private String issuedAt;
        private String documentType;
        private String message;
    }
}
