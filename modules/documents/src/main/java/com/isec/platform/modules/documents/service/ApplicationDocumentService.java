package com.isec.platform.modules.documents.service;

import com.isec.platform.modules.documents.domain.ApplicationDocument;
import com.isec.platform.modules.documents.dto.ApplicationDocumentDto;
import com.isec.platform.modules.documents.repository.ApplicationDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationDocumentService {

    private final S3Service s3Service;
    private final ApplicationDocumentRepository documentRepository;

    private static final List<String> MANDATORY_DOCUMENTS = List.of("LOGBOOK", "NATIONAL_ID", "KRA_PIN", "VALUATION_REPORT");

    @Transactional
    public List<ApplicationDocumentDto> getOrCreatePresignedUrls(Long applicationId) {
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
            String newUrl = s3Service.generatePresignedGetUrl(doc.getS3Key());
            doc.setLastPresignedUrl(newUrl);
            doc.setUrlExpiryAt(LocalDateTime.now().plusHours(1));
            return documentRepository.save(doc);
        }
        return doc;
    }

    private ApplicationDocument createNewDocument(Long applicationId, String type) {
        String s3Key = "apps/" + applicationId + "/" + type.toLowerCase() + "-" + UUID.randomUUID() + ".pdf";
        // For the first time, we provide a PUT URL so they can upload it.
        // Actually, the requirement says "it shd return all the documents url so that front end can use it to render".
        // Rendering usually implies GET. But they also need to upload.
        // Let's provide a GET URL. If they haven't uploaded yet, the GET URL will just return 404 from S3 when accessed.
        // But for "DRAFT" phase, they might need PUT URLs.
        
        String presignedUrl = s3Service.generatePresignedGetUrl(s3Key);
        
        ApplicationDocument doc = ApplicationDocument.builder()
                .applicationId(applicationId)
                .documentType(type)
                .s3Key(s3Key)
                .lastPresignedUrl(presignedUrl)
                .urlExpiryAt(LocalDateTime.now().plusHours(1))
                .build();
        
        return documentRepository.save(doc);
    }
    
    @Transactional
    public String getUploadUrl(Long applicationId, String documentType) {
        ApplicationDocument doc = documentRepository.findByApplicationIdAndDocumentType(applicationId, documentType.toUpperCase())
                .orElseGet(() -> createNewDocument(applicationId, documentType.toUpperCase()));
        
        return s3Service.generatePresignedPutUrl(doc.getS3Key(), "application/pdf");
    }

    private ApplicationDocumentDto mapToDto(ApplicationDocument doc) {
        return ApplicationDocumentDto.builder()
                .documentType(doc.getDocumentType())
                .presignedUrl(doc.getLastPresignedUrl())
                .s3Key(doc.getS3Key())
                .build();
    }
}
