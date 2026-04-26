package com.isec.platform.modules.documents.controller;

import com.isec.platform.modules.documents.service.DocumentVerificationService;
import com.isec.platform.modules.documents.service.DocumentVerificationService.VerificationResult;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/verify")
@RequiredArgsConstructor
@Slf4j
public class VerificationController {

    private final DocumentVerificationService verificationService;

    @GetMapping("/doc/{documentId}")
    public Mono<ResponseEntity<VerificationResponse>> verifyDocument(@PathVariable String documentId) {
        log.info("Public Verification Request: Checking status for documentId={}", documentId);
        UUID uuid;
        try {
            uuid = UUID.fromString(documentId);
        } catch (IllegalArgumentException e) {
            log.warn("Public Verification Failed: Invalid UUID format for documentId={}", documentId);
            return Mono.just(ResponseEntity.badRequest().body(VerificationResponse.builder()
                    .status("NOT_FOUND")
                    .message("Invalid Document ID format")
                    .build()));
        }

        return verificationService.verifyByUuid(uuid)
                .map(result -> ResponseEntity.ok(mapToResponse(result)));
    }

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<VerificationResponse>> verifyUploadedPdf(@RequestPart("file") FilePart filePart) {
        String fileName = filePart.filename();
        log.info("Cryptographic Verification Request: Uploaded file={}", fileName);
        
        return filePart.content()
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    org.springframework.core.io.buffer.DataBufferUtils.release(dataBuffer);
                    return Mono.just(bytes);
                })
                .reduce(new java.io.ByteArrayOutputStream(), (baos, bytes) -> {
                    try {
                        baos.write(bytes);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return baos;
                })
                .map(java.io.ByteArrayOutputStream::toByteArray)
                .flatMap(bytes -> verificationService.verifyByPdfContent(bytes, fileName))
                .map(result -> ResponseEntity.ok(mapToResponse(result)))
                .onErrorResume(e -> {
                    log.error("Cryptographic Verification Error: processing file={} failed", fileName, e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
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
