package com.isec.platform.modules.ocr.application;

import com.isec.platform.modules.ocr.domain.*;
import com.isec.platform.modules.ocr.dto.OcrExtractionResultDto;
import com.isec.platform.modules.ocr.dto.OcrFieldDto;
import com.isec.platform.modules.ocr.dto.ValidationDto;
import com.isec.platform.modules.ocr.repository.OcrAuditLogRepository;
import com.isec.platform.modules.ocr.repository.OcrDocumentRepository;
import com.isec.platform.modules.ocr.repository.OcrFieldRepository;
import com.isec.platform.modules.ocr.infrastructure.messaging.OcrMessageProducer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OcrOrchestratorService {

    private final OcrDocumentRepository documentRepository;
    private final OcrFieldRepository fieldRepository;
    private final OcrAuditLogRepository auditLogRepository;
    private final OcrMessageProducer messageProducer;

    @Transactional
    public UUID submit(UUID tenantId, DocumentType type, MultipartFile file, String s3Url) {
        String sha256 = computeSha256(file);
        Optional<OcrDocument> existing = documentRepository.findByTenantIdAndDocumentHash(tenantId, sha256);
        if (existing.isPresent()) {
            log.info("Duplicate document detected for tenant {} with hash {}", tenantId, sha256);
            return existing.get().getId();
        }

        OcrDocument doc = OcrDocument.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .documentType(type)
                .s3Url(s3Url)
                .documentHash(sha256)
                .status(OcrStatus.RECEIVED)
                .overallConfidence(null)
                .build();
        documentRepository.save(doc);

        audit("SUBMITTED", doc, Map.of("filename", file.getOriginalFilename()));
        messageProducer.publishDocumentSubmitted(doc.getId(), tenantId, type.name(), s3Url, sha256);
        return doc.getId();
    }

    public Optional<OcrExtractionResultDto> getExtraction(UUID id) {
        return documentRepository.findById(id).map(doc -> {
            List<OcrField> fields = fieldRepository.findByDocumentId(doc.getId());
            Map<String, OcrFieldDto> map = new LinkedHashMap<>();
            for (OcrField f : fields) {
                map.put(f.getFieldName(), OcrFieldDto.builder()
                        .value(f.getFieldValue())
                        .confidence(f.getConfidence())
                        .status(f.getStatus() != null ? f.getStatus().name() : null)
                        .build());
            }
            return OcrExtractionResultDto.builder()
                    .documentId(doc.getId())
                    .tenantId(doc.getTenantId())
                    .documentType(doc.getDocumentType().name())
                    .s3Url(doc.getS3Url())
                    .extractedAt(Instant.ofEpochMilli(Optional.ofNullable(doc.getCreatedAt()).map(Instant::toEpochMilli).orElseGet(System::currentTimeMillis)))
                    .overallConfidence(doc.getOverallConfidence() != null ? doc.getOverallConfidence() : average(map))
                    .fields(map)
                    .validation(ValidationDto.builder().isValid(true).errors(List.of()).build())
                    .build();
        });
    }

    private BigDecimal average(Map<String, OcrFieldDto> map) {
        if (map.isEmpty()) return null;
        return map.values().stream()
                .map(OcrFieldDto::getConfidence)
                .filter(Objects::nonNull)
                .map(BigDecimal::doubleValue)
                .mapToDouble(Double::doubleValue)
                .average()
                .stream()
                .mapToObj(avg -> BigDecimal.valueOf(Math.round(avg * 10000.0) / 10000.0))
                .findFirst().orElse(null);
    }

    private void audit(String event, OcrDocument doc, Map<String, Object> meta) {
        String metadataStr = null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            metadataStr = meta != null ? om.writeValueAsString(meta) : null;
        } catch (Exception e) {
            log.warn("Failed to serialize audit metadata: {}", e.getMessage());
        }
        OcrAuditLog logRec = OcrAuditLog.builder()
                .id(UUID.randomUUID())
                .document(doc)
                .event(event)
                .metadata(metadataStr)
                .build();
        auditLogRepository.save(logRec);
    }

    private String computeSha256(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = file.getBytes();
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // fallback hash from original filename if content unavailable
            log.warn("Failed to compute SHA-256 from content, using filename fallback: {}", e.getMessage());
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(Objects.requireNonNullElse(file.getOriginalFilename(), "").getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) sb.append(String.format("%02x", b));
                return sb.toString();
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to compute SHA-256", ex);
            }
        }
    }
}
