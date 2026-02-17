package com.isec.platform.modules.ocr.api;

import com.isec.platform.modules.ocr.application.OcrOrchestratorService;
import com.isec.platform.modules.ocr.domain.DocumentType;
import com.isec.platform.modules.ocr.dto.OcrExtractionResultDto;
import com.isec.platform.modules.ocr.infrastructure.storage.S3StorageService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ocr/documents")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OcrDocumentController {

    private final OcrOrchestratorService orchestratorService;
    private final S3StorageService storageService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> submit(@RequestParam @NotNull UUID tenantId,
                                    @RequestParam @NotNull DocumentType documentType,
                                    @RequestPart("file") MultipartFile file) {
        String s3Url = storageService.upload(tenantId, file, null);
        UUID id = orchestratorService.submit(tenantId, documentType, file, s3Url);
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/ocr/documents/" + id))
                .body(new SubmissionResponse(id, "RECEIVED"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        return orchestratorService.getExtraction(id)
                .map(result -> {
                    if (result.getS3Url() != null && result.getS3Url().startsWith("s3://")) {
                        // Return a presigned URL valid for 30 minutes for viewing
                        String presignedUrl = storageService.generatePresignedUrl(result.getS3Url(), 30);
                        return ResponseEntity.ok(result.toBuilder().s3Url(presignedUrl).build());
                    }
                    return ResponseEntity.ok(result);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/presigned")
    public ResponseEntity<?> presigned(@PathVariable UUID id,
                                       @RequestParam(name = "minutes", required = false, defaultValue = "60") int minutes) {
        return orchestratorService.getExtraction(id)
                .map(result -> {
                    String s3 = result.getS3Url();
                    if (s3 == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No S3 object for document");
                    }
                    String presigned = storageService.generatePresignedUrl(s3, minutes);
                    return ResponseEntity.ok(result.toBuilder().s3Url(presigned).build());
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record SubmissionResponse(UUID id, String status) {}
}
