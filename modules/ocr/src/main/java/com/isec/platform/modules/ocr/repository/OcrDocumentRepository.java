package com.isec.platform.modules.ocr.repository;

import com.isec.platform.modules.ocr.domain.OcrDocument;
import com.isec.platform.modules.ocr.domain.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OcrDocumentRepository extends JpaRepository<OcrDocument, UUID> {
    Optional<OcrDocument> findByTenantIdAndDocumentHash(UUID tenantId, String documentHash);
    long countByDocumentType(DocumentType type);
}
