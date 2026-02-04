package com.isec.platform.modules.certificates.service;

import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.certificates.domain.Certificate;
import com.isec.platform.modules.certificates.domain.CertificateType;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import com.isec.platform.modules.policies.domain.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CertificateService certificateService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldIssueMonth1And2WhenPaid70Percent() {
        Policy policy = Policy.builder()
                .id(1L)
                .applicationId(1L)
                .policyNumber("POL-123")
                .totalAnnualPremium(new BigDecimal("10000"))
                .balance(new BigDecimal("3000")) // Paid 7000
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1).minusDays(1))
                .build();

        Application application = Application.builder()
                .id(1L)
                .registrationNumber("KBC 123X")
                .build();

        when(certificateRepository.findByPolicyId(1L)).thenReturn(new ArrayList<>());
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        certificateService.processCertificateIssuance(policy, new BigDecimal("7000"));

        verify(certificateRepository, times(2)).save(any(Certificate.class));
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void shouldIssueAnnualWhenFullyPaid() {
        Policy policy = Policy.builder()
                .id(1L)
                .applicationId(1L)
                .policyNumber("POL-123")
                .totalAnnualPremium(new BigDecimal("10000"))
                .balance(BigDecimal.ZERO)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1).minusDays(1))
                .build();

        Application application = Application.builder()
                .id(1L)
                .registrationNumber("KBC 123X")
                .build();

        when(certificateRepository.findByPolicyId(1L)).thenReturn(new ArrayList<>());
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        certificateService.processCertificateIssuance(policy, new BigDecimal("10000"));

        verify(certificateRepository).save(argThat(c -> c.getType() == CertificateType.ANNUAL_FULL));
        verify(rabbitTemplate, times(3)).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
