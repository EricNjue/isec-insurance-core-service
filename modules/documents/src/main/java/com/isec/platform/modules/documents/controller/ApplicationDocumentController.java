package com.isec.platform.modules.documents.controller;

import com.isec.platform.modules.documents.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class ApplicationDocumentController {

    @GetMapping("/presigned-url")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @RequestParam String documentType,
            @RequestParam Long applicationId) {
        
        String key = "apps/" + applicationId + "/" + documentType.toLowerCase() + "-" + UUID.randomUUID() + ".pdf";
        // Mocking S3 presigned URL generation
        String mockUrl = "https://isec-insurance-docs.s3.eu-west-1.amazonaws.com/" + key + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&...";
        
        return ResponseEntity.ok(new PresignedUrlResponse(mockUrl, key));
    }
}
