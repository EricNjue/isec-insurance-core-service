package com.isec.platform.modules.ocr.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ocr_field")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrField {

    @Id
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private OcrDocument document;

    @Column(name = "field_name", length = 100, nullable = false)
    private String fieldName;

    @Column(name = "field_value", columnDefinition = "TEXT")
    private String fieldValue;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private FieldStatus status;
}
