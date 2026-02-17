package com.isec.platform.modules.ocr.repository;

import com.isec.platform.modules.ocr.domain.OcrField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OcrFieldRepository extends JpaRepository<OcrField, UUID> {
    List<OcrField> findByDocumentId(UUID documentId);
}
