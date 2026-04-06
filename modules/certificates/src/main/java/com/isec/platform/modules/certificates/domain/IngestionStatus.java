package com.isec.platform.modules.certificates.domain;

public enum IngestionStatus {
    RECEIVED,
    PARSED,
    MATCHED,
    STORED,
    COMPLETED,
    FAILED,
    MANUAL_REVIEW_REQUIRED,
    DUPLICATE_IGNORED
}
