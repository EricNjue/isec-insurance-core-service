package com.isec.platform.modules.certificates.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.certificates.adapters.CertificateProviderAdapter;
import com.isec.platform.modules.certificates.domain.canonical.CertificateRequest;
import com.isec.platform.modules.certificates.domain.canonical.CertificateResponse;
import com.isec.platform.modules.certificates.domain.canonical.CertificateStatus;
import com.isec.platform.modules.certificates.domain.canonical.CertificateType;
import com.isec.platform.modules.certificates.domain.canonical.CustomerDetails;
import com.isec.platform.modules.certificates.domain.canonical.Money;
import com.isec.platform.modules.certificates.domain.canonical.PolicyDetails;
import com.isec.platform.modules.certificates.domain.canonical.ProviderType;
import com.isec.platform.modules.certificates.domain.canonical.VehicleDetails;
import com.isec.platform.modules.certificates.domain.entity.CertificateEntity;
import com.isec.platform.modules.certificates.metrics.ProviderMetricsRecorder;
import com.isec.platform.modules.certificates.registry.CertificateProviderRegistry;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class CertificateApplicationServiceTest {

    private CertificateProviderRegistry providerRegistry;
    private CertificateRepository certificateRepository;
    private ProviderMetricsRecorder metricsRecorder;
    private CertificateApplicationService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        providerRegistry = mock(CertificateProviderRegistry.class);
        certificateRepository = mock(CertificateRepository.class);
        metricsRecorder = mock(ProviderMetricsRecorder.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new CertificateApplicationService(providerRegistry, certificateRepository, objectMapper, metricsRecorder);
        TenantContext.setTenantId("APA");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void issueCertificate_returnsStoredResponseOnIdempotencyHit() throws Exception {
        CertificateResponse storedResponse = new CertificateResponse(ProviderType.APA, CertificateStatus.ISSUED, "CERT-1", "EXT-1", "OK", Instant.now());
        String payload = objectMapper.writeValueAsString(storedResponse);

        CertificateEntity existing = CertificateEntity.builder()
                .idempotencyKey("idem-1")
                .responsePayload(payload)
                .build();

        when(certificateRepository.findByTenantIdAndIdempotencyKey("APA", "idem-1"))
                .thenReturn(Optional.of(existing));

        CertificateRequest request = buildRequest("idem-1", CertificateType.ANNUAL_FULL);

        CertificateResponse response = service.issueCertificate(request);

        assertEquals(storedResponse.certificateNumber(), response.certificateNumber());
        verifyNoInteractions(providerRegistry);
    }

    @Test
    void issueCertificate_persistsCertificateType() {
        CertificateProviderAdapter adapter = mock(CertificateProviderAdapter.class);
        when(adapter.providerType()).thenReturn(ProviderType.APA);

        CertificateResponse providerResponse = new CertificateResponse(ProviderType.APA, CertificateStatus.ISSUED, "CERT-2", "EXT-2", "OK", Instant.now());
        when(adapter.issueCertificate(any())).thenReturn(providerResponse);

        when(providerRegistry.resolveProvider(eq("APA"), any())).thenReturn(adapter);
        when(certificateRepository.findByTenantIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());

        CertificateRequest request = buildRequest("idem-2", CertificateType.MONTH_1);

        service.issueCertificate(request);

        ArgumentCaptor<CertificateEntity> captor = ArgumentCaptor.forClass(CertificateEntity.class);
        verify(certificateRepository).save(captor.capture());
        assertEquals(CertificateType.MONTH_1, captor.getValue().getCertificateType());
        assertEquals("APA", captor.getValue().getTenantId());
        assertNotNull(captor.getValue().getRequestPayload());
    }

    private CertificateRequest buildRequest(String idempotencyKey, CertificateType certificateType) {
        return new CertificateRequest(
                idempotencyKey,
                certificateType,
                ProviderType.APA,
                new PolicyDetails("POL-1", LocalDate.now(), LocalDate.now().plusMonths(1), null),
                new CustomerDetails("Jane", "Doe", null, "jane@example.com", "+254700000000", null, null, null),
                new VehicleDetails("KDA 123A", "Toyota", "RAV4", null, null, null),
                new Money(new BigDecimal("1000.00"), "KES")
        );
    }
}
