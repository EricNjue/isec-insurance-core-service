package com.isec.platform.modules.certificates.service;

import com.isec.platform.modules.certificates.domain.Certificate;
import com.isec.platform.modules.certificates.domain.CertificateType;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import com.isec.platform.modules.policies.domain.Policy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private static final BigDecimal MONTHLY_PREMIUM_RATE = new BigDecimal("0.35");

    @Transactional
    public void processCertificateIssuance(Policy policy, BigDecimal amountPaid) {
        BigDecimal annualPremium = policy.getTotalAnnualPremium();
        BigDecimal monthlyRequirement = annualPremium.multiply(MONTHLY_PREMIUM_RATE).setScale(2, RoundingMode.HALF_UP);
        
        List<Certificate> issued = certificateRepository.findByPolicyId(policy.getId());
        
        // Month 1 & 2 logic
        if (amountPaid.compareTo(monthlyRequirement) >= 0 && issued.isEmpty()) {
            issueCertificate(policy, CertificateType.MONTH_1, policy.getStartDate(), policy.getStartDate().plusMonths(1).minusDays(1));
            // Check if they paid enough for Month 2 as well
            if (amountPaid.compareTo(monthlyRequirement.multiply(new BigDecimal("2"))) >= 0) {
                issueCertificate(policy, CertificateType.MONTH_2, policy.getStartDate().plusMonths(1), policy.getStartDate().plusMonths(2).minusDays(1));
            }
        }
        
        // Month 3 (Annual Remainder) logic - Only if balance is zero
        if (policy.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            boolean hasMonth1 = issued.stream().anyMatch(c -> c.getType() == CertificateType.MONTH_1);
            if (hasMonth1) {
                issueCertificate(policy, CertificateType.ANNUAL_REMAINDER, policy.getStartDate().plusMonths(2), policy.getExpiryDate());
            } else {
                issueCertificate(policy, CertificateType.ANNUAL_FULL, policy.getStartDate(), policy.getExpiryDate());
            }
        }
    }

    private void issueCertificate(Policy policy, CertificateType type, LocalDate start, LocalDate end) {
        Certificate cert = Certificate.builder()
                .policyId(policy.getId())
                .type(type)
                .startDate(start)
                .expiryDate(end)
                .issuedAt(LocalDateTime.now())
                .build();
        certificateRepository.save(cert);
        // In real world, trigger DMVIC integration here via RabbitMQ
    }
}
