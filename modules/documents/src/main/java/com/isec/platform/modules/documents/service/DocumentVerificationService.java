package com.isec.platform.modules.documents.service;

import com.isec.platform.modules.documents.domain.ValuationLetter;
import com.isec.platform.modules.documents.repository.ValuationLetterRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentVerificationService {

    private final ValuationLetterRepository letterRepository;
    private final PdfSecurityService pdfSecurityService;

    @Value("${documents.valuation-letter.validity-days:14}")
    private int validityDays;

    public Mono<VerificationResult> verifyByUuid(UUID uuid) {
        log.info("Verifying document by UUID: {}", uuid);
        return letterRepository.findByDocumentUuid(uuid)
                .flatMap(this::processVerification)
                .defaultIfEmpty(VerificationResult.builder()
                        .status("NOT_FOUND")
                        .message("Document not found in our records")
                        .build());
    }

    public Mono<VerificationResult> verifyByPdfContent(byte[] content, String fileName) {
        log.info("Verifying document by PDF content. File: {}", fileName);
        return Mono.fromCallable(() -> {
                    String hash = pdfSecurityService.calculateHash(content);
                    java.util.Map<String, String> metadata = pdfSecurityService.extractMetadata(content);
                    return new Object[] { hash, metadata };
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(args -> {
                    String hash = (String) args[0];
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, String> metadata = (java.util.Map<String, String>) args[1];

                    String docIdStr = metadata.get("documentId");
                    if (docIdStr == null) {
                        log.warn("Verification Failed: No metadata found in PDF {}", fileName);
                        return Mono.just(VerificationResult.builder()
                                .status("MODIFIED")
                                .message("No document metadata found in the PDF")
                                .build());
                    }

                    try {
                        UUID uuid = UUID.fromString(docIdStr);
                        return letterRepository.findByDocumentUuid(uuid)
                                .flatMap(letter -> {
                                    if (letter.getDocumentHash().equals(hash)) {
                                        return processVerification(letter)
                                                .map(result -> result.toBuilder()
                                                        .message("Cryptographic hash matches record")
                                                        .build());
                                    } else {
                                        log.error("Verification ALERT: documentId={} hash MISMATCH. Possible tampering detected!", uuid);
                                        return Mono.just(VerificationResult.builder()
                                                .documentId(letter.getDocumentUuid().toString())
                                                .status("MODIFIED")
                                                .message("PDF content does not match the original hash")
                                                .build());
                                    }
                                })
                                .defaultIfEmpty(VerificationResult.builder()
                                        .status("NOT_FOUND")
                                        .message("Document metadata exists but record not found")
                                        .build());
                    } catch (IllegalArgumentException e) {
                        log.error("Verification Error: Invalid UUID in PDF {}", fileName, e);
                        return Mono.just(VerificationResult.builder()
                                .status("MODIFIED")
                                .message("Invalid document ID in PDF metadata")
                                .build());
                    }
                });
    }

    private Mono<VerificationResult> processVerification(ValuationLetter letter) {
        String status = resolveStatus(letter);
        Mono<ValuationLetter> updateMono = Mono.just(letter);
        
        if ("EXPIRED".equals(status) && letter.getStatus() != ValuationLetter.ValuationLetterStatus.EXPIRED) {
            letter.setStatus(ValuationLetter.ValuationLetterStatus.EXPIRED);
            updateMono = letterRepository.save(letter)
                    .doOnNext(l -> log.info("Document verification: Updated documentId={} status to EXPIRED in database", letter.getDocumentUuid()));
        }

        return updateMono.map(l -> VerificationResult.builder()
                .documentId(l.getDocumentUuid().toString())
                .status(status)
                .issuedAt(l.getGeneratedAt() != null ? l.getGeneratedAt().toString() : null)
                .documentType(l.getDocumentType())
                .build());
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
