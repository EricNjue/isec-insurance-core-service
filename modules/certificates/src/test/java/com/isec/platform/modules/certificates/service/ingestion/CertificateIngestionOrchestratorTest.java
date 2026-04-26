package com.isec.platform.modules.certificates.service.ingestion;

import com.isec.platform.modules.certificates.domain.CertificateIngestionAudit;
import com.isec.platform.modules.certificates.domain.IngestionStatus;
import com.isec.platform.modules.certificates.repository.CertificateIngestionAuditRepository;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import com.isec.platform.modules.documents.service.S3Service;
import com.isec.platform.modules.policies.repository.PolicyRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CertificateIngestionOrchestratorTest {

    private CertificateIngestionOrchestrator orchestrator;

    @Mock
    private CertificateIngestionAuditRepository auditRepository;
    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private PolicyRepository policyRepository;
    @Mock
    private S3Service s3Service;
    @Mock
    private MimeMessage mockMessage;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orchestrator = new CertificateIngestionOrchestrator(
                Collections.emptyList(),
                certificateRepository,
                policyRepository,
                auditRepository,
                s3Service
        );
    }

    @Test
    void processEmailAsync_ShouldSkipIfAlreadyClaimed() {
        String messageId = "msg123";
        when(auditRepository.updateStatusAtomic(messageId, IngestionStatus.RECEIVED.name(), IngestionStatus.PROCESSING.name())).thenReturn(Mono.just(0));

        orchestrator.processEmailAsync(mockMessage, messageId);

        verify(auditRepository, never()).findByEmailMessageId(anyString());
    }

    @Test
    void processEmailAsync_ShouldProceedIfClaimedSuccessfully() {
        String messageId = "msg123";
        when(auditRepository.updateStatusAtomic(messageId, IngestionStatus.RECEIVED.name(), IngestionStatus.PROCESSING.name())).thenReturn(Mono.just(1));
        when(auditRepository.findByEmailMessageId(messageId)).thenReturn(Mono.just(new CertificateIngestionAudit()));

        orchestrator.processEmailAsync(mockMessage, messageId);

        verify(auditRepository).findByEmailMessageId(messageId);
    }
}
