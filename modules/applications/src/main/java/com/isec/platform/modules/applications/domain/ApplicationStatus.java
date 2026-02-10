package com.isec.platform.modules.applications.domain;

public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    QUOTED,
    UNDERWRITING_REVIEW,
    APPROVED_PENDING_PAYMENT,
    DOCUMENTS_UPLOADED,
    PAYMENT_PENDING,
    PARTIALLY_PAID,
    FULLY_PAID,
    POLICY_ISSUED,
    CANCELLED,
    DECLINED
}
