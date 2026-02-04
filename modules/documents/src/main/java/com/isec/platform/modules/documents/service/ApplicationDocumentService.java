package com.isec.platform.modules.documents.service;

import com.isec.platform.modules.documents.domain.ApplicationDocument;
import com.isec.platform.modules.documents.dto.ApplicationDocumentDto;
import com.isec.platform.modules.documents.repository.ApplicationDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationDocumentService {

    private final S3Service s3Service;
    private final ApplicationDocumentRepository documentRepository;

    private static final List<String> MANDATORY_DOCUMENTS = List.of("LOGBOOK", "NATIONAL_ID", "KRA_PIN", "VALUATION_REPORT");

    @Transactional
    public List<ApplicationDocumentDto> getOrCreatePresignedUrls(Long applicationId) {
        log.debug("Retrieving/Creating presigned URLs for application: {}", applicationId);
        return MANDATORY_DOCUMENTS.stream()
                .map(type -> getOrCreateDocument(applicationId, type))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ApplicationDocument getOrCreateDocument(Long applicationId, String type) {
        return documentRepository.findByApplicationIdAndDocumentType(applicationId, type)
                .map(this::refreshUrlIfExpired)
                .orElseGet(() -> createNewDocument(applicationId, type));
    }

    private ApplicationDocument refreshUrlIfExpired(ApplicationDocument doc) {
        if (doc.getUrlExpiryAt() == null || doc.getUrlExpiryAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
            log.info("Presigned URL for document {} of application {} is expired or near expiry. Refreshing...", 
                    doc.getDocumentType(), doc.getApplicationId());
            String newUrl = s3Service.generatePresignedGetUrl(doc.getS3Key());
            doc.setLastPresignedUrl(newUrl);
            doc.setUrlExpiryAt(LocalDateTime.now().plusHours(1));
            return documentRepository.save(doc);
        }
        return doc;
    }

    private ApplicationDocument createNewDocument(Long applicationId, String type) {
        log.info("Creating new document record for application: {} type: {}", applicationId, type);
        String s3Key = "apps/" + applicationId + "/" + type.toLowerCase() + "-" + UUID.randomUUID() + ".pdf";
        
        String presignedUrl = s3Service.generatePresignedGetUrl(s3Key);
        
        ApplicationDocument doc = ApplicationDocument.builder()
                .applicationId(applicationId)
                .documentType(type)
                .s3Key(s3Key)
                .lastPresignedUrl(presignedUrl)
                .urlExpiryAt(LocalDateTime.now().plusHours(1))
                .build();
        
        ApplicationDocument saved = documentRepository.save(doc);
        log.info("Document record created with ID: {} and S3 Key: {}", saved.getId(), saved.getS3Key());
        return saved;
    }
    
    @Transactional
    public String getUploadUrl(Long applicationId, String documentType) {
        log.info("Generating upload URL for application: {} type: {}", applicationId, documentType);
        ApplicationDocument doc = documentRepository.findByApplicationIdAndDocumentType(applicationId, documentType.toUpperCase())
                .orElseGet(() -> createNewDocument(applicationId, documentType.toUpperCase()));
        
        String uploadUrl = s3Service.generatePresignedPutUrl(doc.getS3Key(), "application/pdf");
        log.debug("Upload URL generated successfully");
        return uploadUrl;
    }

    private ApplicationDocumentDto mapToDto(ApplicationDocument doc) {
        return ApplicationDocumentDto.builder()
                .documentType(doc.getDocumentType())
                .presignedUrl(doc.getLastPresignedUrl())
                .s3Key(doc.getS3Key())
                .build();
    }
}
