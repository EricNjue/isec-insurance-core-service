package com.isec.platform.modules.documents.service;

import com.isec.platform.modules.documents.domain.ApplicationDocument;
import com.isec.platform.modules.documents.dto.ApplicationDocumentDto;
import com.isec.platform.modules.documents.repository.ApplicationDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationDocumentService {

    private final S3Service s3Service;
    private final ApplicationDocumentRepository documentRepository;

    private static final List<String> MANDATORY_DOCUMENTS = List.of("LOGBOOK", "NATIONAL_ID", "KRA_PIN", "VALUATION_REPORT","VEHICLE_PHOTO_1","VEHICLE_PHOTO_2","VEHICLE_PHOTO_3","VEHICLE_PHOTO_4");

    public Mono<List<ApplicationDocumentDto>> getOrCreatePresignedUrls(Long applicationId) {
        log.debug("Retrieving/Creating presigned URLs for application: {}", applicationId);
        if (applicationId == null) {
            return Flux.fromIterable(MANDATORY_DOCUMENTS)
                    .map(type -> {
                        String s3Key = "quotes/new/" + type.toLowerCase() + "-" + UUID.randomUUID() + ".pdf";
                        String presignedUrl = s3Service.generatePresignedPutUrl(s3Key, "application/pdf");
                        return ApplicationDocumentDto.builder()
                                .documentType(type)
                                .presignedUrl(presignedUrl)
                                .s3Key(s3Key)
                                .build();
                    })
                    .collectList();
        }
        return Flux.fromIterable(MANDATORY_DOCUMENTS)
                .flatMap(type -> getOrCreateDocument(applicationId, type))
                .map(this::mapToDto)
                .collectList();
    }

    public Mono<Void> linkDocumentsToApplication(Long applicationId, List<ApplicationDocumentDto> documentDtos) {
        if (documentDtos == null || documentDtos.isEmpty()) {
            log.debug("No documents to link for application: {}", applicationId);
            return Mono.empty();
        }

        log.info("Linking {} documents to application: {}", documentDtos.size(), applicationId);
        List<ApplicationDocument> documents = documentDtos.stream()
                .map(dto -> ApplicationDocument.builder()
                        .applicationId(applicationId)
                        .documentType(dto.getDocumentType())
                        .s3Key(dto.getS3Key())
                        .lastPresignedUrl(dto.getPresignedUrl())
                        .urlExpiryAt(LocalDateTime.now().plusHours(1))
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();

        return documentRepository.saveAll(documents).then();
    }

    private Mono<ApplicationDocument> getOrCreateDocument(Long applicationId, String type) {
        return documentRepository.findByApplicationIdAndDocumentType(applicationId, type)
                .flatMap(this::refreshUrlIfExpired)
                .switchIfEmpty(Mono.defer(() -> createNewDocument(applicationId, type)));
    }

    private Mono<ApplicationDocument> refreshUrlIfExpired(ApplicationDocument doc) {
        if (doc.getUrlExpiryAt() == null || doc.getUrlExpiryAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
            log.info("Presigned URL for document {} of application {} is expired or near expiry. Refreshing...", 
                    doc.getDocumentType(), doc.getApplicationId());
            String newUrl = s3Service.generatePresignedGetUrl(doc.getS3Key());
            doc.setLastPresignedUrl(newUrl);
            doc.setUrlExpiryAt(LocalDateTime.now().plusHours(1));
            return documentRepository.save(doc);
        }
        return Mono.just(doc);
    }

    private Mono<ApplicationDocument> createNewDocument(Long applicationId, String type) {
        log.info("Creating new document record for application: {} type: {}", applicationId, type);
        String s3Key = "apps/" + applicationId + "/" + type.toLowerCase() + "-" + UUID.randomUUID() + ".pdf";
        
        String presignedUrl = s3Service.generatePresignedGetUrl(s3Key);
        
        ApplicationDocument doc = ApplicationDocument.builder()
                .applicationId(applicationId)
                .documentType(type)
                .s3Key(s3Key)
                .lastPresignedUrl(presignedUrl)
                .urlExpiryAt(LocalDateTime.now().plusHours(1))
                .createdAt(LocalDateTime.now())
                .build();
        
        return documentRepository.save(doc)
                .doOnNext(saved -> log.info("Document record created with ID: {} and S3 Key: {}", saved.getId(), saved.getS3Key()));
    }
    
    public Mono<String> getUploadUrl(Long applicationId, String documentType) {
        log.info("Generating upload URL for application: {} type: {}", applicationId, documentType);
        return documentRepository.findByApplicationIdAndDocumentType(applicationId, documentType.toUpperCase())
                .switchIfEmpty(Mono.defer(() -> createNewDocument(applicationId, documentType.toUpperCase())))
                .map(doc -> {
                    String uploadUrl = s3Service.generatePresignedPutUrl(doc.getS3Key(), "application/pdf");
                    log.debug("Upload URL generated successfully");
                    return uploadUrl;
                });
    }

    private ApplicationDocumentDto mapToDto(ApplicationDocument doc) {
        return ApplicationDocumentDto.builder()
                .documentType(doc.getDocumentType())
                .presignedUrl(doc.getLastPresignedUrl())
                .s3Key(doc.getS3Key())
                .build();
    }
}
