package com.isec.platform.modules.certificates.messaging;

import com.isec.platform.common.idempotency.service.IdempotencyService;
import com.isec.platform.messaging.events.CertificateRequestedEvent;
import com.isec.platform.modules.certificates.domain.Certificate;
import com.isec.platform.modules.certificates.domain.CertificateStatus;
import com.isec.platform.modules.certificates.domain.CertificateType;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import com.isec.platform.modules.integrations.dmvic.DmvicClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateRequestConsumerTest {

    @Mock
    private DmvicClient dmvicClient;
    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private CertificateRequestConsumer consumer;

    private CertificateRequestedEvent event;
    private Certificate certificate;

    @BeforeEach
    void setUp() {
        event = CertificateRequestedEvent.builder()
                .eventId("event-123")
                .policyId(1L)
                .policyNumber("POL-001")
                .registrationNumber("KAA 001A")
                .certificateType("MONTH_1")
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusMonths(1))
                .recipientEmail("test@example.com")
                .recipientPhoneNumber("254712345678")
                .correlationId("corr-123")
                .build();

        certificate = Certificate.builder()
                .id(1L)
                .policyId(1L)
                .type(CertificateType.MONTH_1)
                .status(CertificateStatus.PENDING)
                .idempotencyKey("event-123")
                .build();
    }

    @Test
    void handleCertificateRequest_ShouldProcess_WhenNotDuplicate() {
        when(idempotencyService.isDuplicate(anyString())).thenReturn(false);
        when(certificateRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.of(certificate));
        when(dmvicClient.issueCertificate(anyString(), anyString())).thenReturn("DMVIC-REF");

        consumer.handleCertificateRequest(event);

        verify(dmvicClient).issueCertificate(eq("KAA 001A"), eq("POL-001"));
        verify(certificateRepository, atLeastOnce()).save(any(Certificate.class));
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void handleCertificateRequest_ShouldSkip_WhenDuplicate() {
        when(idempotencyService.isDuplicate(anyString())).thenReturn(true);

        consumer.handleCertificateRequest(event);

        verify(dmvicClient, never()).issueCertificate(anyString(), anyString());
        verify(certificateRepository, never()).save(any(Certificate.class));
    }
}
