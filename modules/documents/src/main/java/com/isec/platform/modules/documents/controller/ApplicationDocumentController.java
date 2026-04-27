package com.isec.platform.modules.documents.controller;

import com.isec.platform.modules.documents.dto.ApplicationDocumentDto;
import com.isec.platform.modules.documents.dto.PresignedUrlResponse;
import com.isec.platform.modules.documents.service.ApplicationDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class ApplicationDocumentController {

    private final ApplicationDocumentService documentService;

    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public Mono<List<ApplicationDocumentDto>> getApplicationDocuments(@PathVariable Long applicationId) {
        return documentService.getOrCreatePresignedUrls(applicationId);
    }

    @GetMapping("/presigned-url")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT')")
    public Mono<PresignedUrlResponse> getUploadUrl(
            @RequestParam String documentType,
            @RequestParam Long applicationId) {
        
        return documentService.getUploadUrl(applicationId, documentType)
                .map(presignedUrl -> new PresignedUrlResponse(presignedUrl, "See Application Documents for S3 Key"));
    }
}
