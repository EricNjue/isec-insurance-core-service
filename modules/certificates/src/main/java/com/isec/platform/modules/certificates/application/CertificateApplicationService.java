package com.isec.platform.modules.certificates.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.certificates.adapters.CertificateProviderAdapter;
import com.isec.platform.modules.certificates.domain.canonical.CertificateRequest;
import com.isec.platform.modules.certificates.domain.canonical.CertificateResponse;
import com.isec.platform.modules.certificates.domain.canonical.CertificateStatus;
import com.isec.platform.modules.certificates.domain.canonical.ProviderType;
import com.isec.platform.modules.certificates.domain.entity.CertificateEntity;
import com.isec.platform.modules.certificates.exception.ProviderException;
import com.isec.platform.modules.certificates.exception.ProviderMappingException;
import com.isec.platform.modules.certificates.metrics.ProviderMetricsRecorder;
import com.isec.platform.modules.certificates.registry.CertificateProviderRegistry;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateApplicationService {

    private final CertificateProviderRegistry providerRegistry;
    private final CertificateRepository certificateRepository;
    private final ObjectMapper objectMapper;
    private final ProviderMetricsRecorder metricsRecorder;

    @Transactional
    public CertificateResponse issueCertificate(CertificateRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ProviderMappingException("Tenant ID is required for certificate issuance");
        }

        // Idempotency is enforced per tenant using the stored certificate aggregate.
        CertificateEntity existing = certificateRepository
                .findByTenantIdAndIdempotencyKey(tenantId, request.idempotencyKey())
                .orElse(null);
        if (existing != null && existing.getResponsePayload() != null) {
            return deserializeResponse(existing.getResponsePayload());
        }

        CertificateProviderAdapter adapter = providerRegistry.resolveProvider(tenantId, request.providerType());
        ProviderType providerType = adapter.providerType();

        Instant start = Instant.now();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("provider_code", providerType.name())) {
            CertificateResponse response = adapter.issueCertificate(request);
            persistCertificate(existing, tenantId, request, response, providerType);
            metricsRecorder.recordSuccess(providerType, Duration.between(start, Instant.now()));
            return response;
        } catch (ProviderException ex) {
            metricsRecorder.recordFailure(providerType, Duration.between(start, Instant.now()));
            persistFailure(existing, tenantId, request, providerType, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            metricsRecorder.recordFailure(providerType, Duration.between(start, Instant.now()));
            persistFailure(existing, tenantId, request, providerType, ex.getMessage());
            throw new ProviderException("Unexpected provider failure", ex);
        }
    }

    @Transactional(readOnly = true)
    public CertificateResponse checkStatus(String externalReference, ProviderType providerType) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ProviderMappingException("Tenant ID is required for status check");
        }

        ProviderType resolvedProvider = providerType;
        if (resolvedProvider == null) {
            resolvedProvider = certificateRepository.findByTenantIdAndExternalReference(tenantId, externalReference)
                    .map(CertificateEntity::getProviderCode)
                    .orElseThrow(() -> new ProviderMappingException("Unable to resolve provider for reference " + externalReference));
        }

        CertificateProviderAdapter adapter = providerRegistry.resolveProvider(tenantId, resolvedProvider);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("provider_code", adapter.providerType().name())) {
            return adapter.checkStatus(externalReference);
        }
    }

    private void persistCertificate(CertificateEntity existing,
                                    String tenantId,
                                    CertificateRequest request,
                                    CertificateResponse response,
                                    ProviderType providerType) {
        CertificateEntity entity = existing != null ? existing : new CertificateEntity();
        entity.setProviderCode(providerType);
        entity.setCertificateType(request.certificateType());
        entity.setCertificateNumber(response.certificateNumber());
        entity.setExternalReference(response.externalReference());
        entity.setStatus(response.status());
        entity.setPremiumAmount(request.premium().amount());
        entity.setCurrency(request.premium().currency());
        entity.setIdempotencyKey(request.idempotencyKey());
        entity.setRequestPayload(serialize(request));
        entity.setResponsePayload(serialize(response));
        entity.setIssuedAt(response.issuedAt());

        if (entity.getTenantId() == null) {
            entity.setTenantId(tenantId);
        }
        certificateRepository.save(entity);
        log.info("Certificate persisted for tenant {} with provider {}", tenantId, providerType);
    }

    private void persistFailure(CertificateEntity existing,
                                String tenantId,
                                CertificateRequest request,
                                ProviderType providerType,
                                String message) {
        CertificateEntity entity = existing != null ? existing : new CertificateEntity();
        entity.setProviderCode(providerType);
        entity.setCertificateType(request.certificateType());
        entity.setStatus(CertificateStatus.FAILED);
        entity.setPremiumAmount(request.premium().amount());
        entity.setCurrency(request.premium().currency());
        entity.setIdempotencyKey(request.idempotencyKey());
        entity.setRequestPayload(serialize(request));
        entity.setResponsePayload(serialize(new CertificateResponse(providerType, CertificateStatus.FAILED, null, null, message, null)));

        if (entity.getTenantId() == null) {
            entity.setTenantId(tenantId);
        }
        certificateRepository.save(entity);
        log.warn("Certificate failure persisted for tenant {} with provider {}", tenantId, providerType);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ProviderException("Unable to serialize payload", ex);
        }
    }

    private CertificateResponse deserializeResponse(String payload) {
        try {
            return objectMapper.readValue(payload, CertificateResponse.class);
        } catch (JsonProcessingException ex) {
            throw new ProviderException("Unable to deserialize stored response", ex);
        }
    }
}
