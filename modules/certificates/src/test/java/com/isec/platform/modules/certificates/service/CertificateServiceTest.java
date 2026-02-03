package com.isec.platform.modules.certificates.service;

import com.isec.platform.modules.certificates.domain.Certificate;
import com.isec.platform.modules.certificates.domain.CertificateType;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import com.isec.platform.modules.policies.domain.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

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
                .totalAnnualPremium(new BigDecimal("10000"))
                .balance(new BigDecimal("3000")) // Paid 7000
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1).minusDays(1))
                .build();

        when(certificateRepository.findByPolicyId(1L)).thenReturn(new ArrayList<>());

        certificateService.processCertificateIssuance(policy, new BigDecimal("7000"));

        verify(certificateRepository, times(2)).save(any(Certificate.class));
    }

    @Test
    void shouldIssueAnnualWhenFullyPaid() {
        Policy policy = Policy.builder()
                .id(1L)
                .totalAnnualPremium(new BigDecimal("10000"))
                .balance(BigDecimal.ZERO)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1).minusDays(1))
                .build();

        when(certificateRepository.findByPolicyId(1L)).thenReturn(new ArrayList<>());

        certificateService.processCertificateIssuance(policy, new BigDecimal("10000"));

        verify(certificateRepository).save(argThat(c -> c.getType() == CertificateType.ANNUAL_FULL));
    }
}
