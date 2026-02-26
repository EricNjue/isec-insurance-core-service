package com.isec.platform.modules.certificates.domain.canonical;

/**
 * Business-level certificate issuance types for monthly/annual coverage flows.
 */
public enum CertificateType {
    MONTH_1,
    MONTH_2,
    ANNUAL_REMAINDER,
    ANNUAL_FULL
}
