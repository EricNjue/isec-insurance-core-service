package com.isec.platform.modules.documents.controller;

import com.isec.platform.modules.documents.dto.PresignedUrlResponse;
import com.isec.platform.modules.documents.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class ApplicationDocumentController {

    private final S3Service s3Service;

    @GetMapping("/presigned-url")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @RequestParam String documentType,
            @RequestParam Long applicationId) {
        
        String key = "apps/" + applicationId + "/" + documentType.toLowerCase() + "-" + UUID.randomUUID() + ".pdf";
        String presignedUrl = s3Service.generatePresignedUrl(key, "application/pdf");
        
        return ResponseEntity.ok(new PresignedUrlResponse(presignedUrl, key));
    }
}
