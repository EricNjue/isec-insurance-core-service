package com.isec.platform.modules.documents.controller;

import com.isec.platform.modules.documents.service.DocumentVerificationService;
import com.isec.platform.modules.documents.service.DocumentVerificationService.VerificationResult;
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

    private final DocumentVerificationService verificationService;

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

        VerificationResult result = verificationService.verifyByUuid(uuid);
        return ResponseEntity.ok(mapToResponse(result));
    }

    @PostMapping("/upload")
    public ResponseEntity<VerificationResponse> verifyUploadedPdf(@RequestParam("file") MultipartFile file) {
        String fileName = file.getOriginalFilename();
        log.info("Cryptographic Verification Request: Uploaded file={}", fileName);
        try {
            VerificationResult result = verificationService.verifyByPdfContent(file.getBytes(), fileName);
            return ResponseEntity.ok(mapToResponse(result));
        } catch (IOException e) {
            log.error("Cryptographic Verification Error: processing file={} failed", fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private VerificationResponse mapToResponse(VerificationResult result) {
        return VerificationResponse.builder()
                .documentId(result.getDocumentId())
                .status(result.getStatus())
                .issuedAt(result.getIssuedAt())
                .documentType(result.getDocumentType())
                .message(result.getMessage())
                .build();
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
