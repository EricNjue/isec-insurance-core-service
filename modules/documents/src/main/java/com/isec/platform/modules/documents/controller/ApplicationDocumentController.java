package com.isec.platform.modules.documents.controller;

import com.isec.platform.modules.documents.dto.ApplicationDocumentDto;
import com.isec.platform.modules.documents.dto.PresignedUrlResponse;
import com.isec.platform.modules.documents.service.ApplicationDocumentService;
import com.isec.platform.modules.documents.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class ApplicationDocumentController {

    private final ApplicationDocumentService documentService;

    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<List<ApplicationDocumentDto>> getApplicationDocuments(@PathVariable Long applicationId) {
        return ResponseEntity.ok(documentService.getOrCreatePresignedUrls(applicationId));
    }

    @GetMapping("/presigned-url")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public ResponseEntity<PresignedUrlResponse> getUploadUrl(
            @RequestParam String documentType,
            @RequestParam Long applicationId) {
        
        String presignedUrl = documentService.getUploadUrl(applicationId, documentType);
        return ResponseEntity.ok(new PresignedUrlResponse(presignedUrl, "See Application Documents for S3 Key"));
    }
}
