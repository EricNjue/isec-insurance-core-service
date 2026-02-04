package com.isec.platform.modules.certificates.service;

import com.isec.platform.modules.certificates.domain.Certificate;
import com.isec.platform.modules.certificates.domain.CertificateType;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import com.isec.platform.modules.policies.domain.Policy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private static final BigDecimal MONTHLY_PREMIUM_RATE = new BigDecimal("0.35");

    @Transactional
    public void processCertificateIssuance(Policy policy, BigDecimal amountPaid) {
        log.info("Processing certificate issuance for policy: {}, amount paid: {}", policy.getPolicyNumber(), amountPaid);
        
        BigDecimal annualPremium = policy.getTotalAnnualPremium();
        BigDecimal monthlyRequirement = annualPremium.multiply(MONTHLY_PREMIUM_RATE).setScale(2, RoundingMode.HALF_UP);
        
        List<Certificate> issued = certificateRepository.findByPolicyId(policy.getId());
        
        // Month 1 & 2 logic
        if (amountPaid.compareTo(monthlyRequirement) >= 0 && issued.isEmpty()) {
            log.info("Policy {} qualified for Month 1 certificate", policy.getPolicyNumber());
            issueCertificate(policy, CertificateType.MONTH_1, policy.getStartDate(), policy.getStartDate().plusMonths(1).minusDays(1));
            // Check if they paid enough for Month 2 as well
            if (amountPaid.compareTo(monthlyRequirement.multiply(new BigDecimal("2"))) >= 0) {
                log.info("Policy {} qualified for Month 2 certificate", policy.getPolicyNumber());
                issueCertificate(policy, CertificateType.MONTH_2, policy.getStartDate().plusMonths(1), policy.getStartDate().plusMonths(2).minusDays(1));
            }
        }
        
        // Month 3 (Annual Remainder) logic - Only if balance is zero
        if (policy.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Policy {} is fully paid. Issuing final certificates", policy.getPolicyNumber());
            boolean hasMonth1 = issued.stream().anyMatch(c -> c.getType() == CertificateType.MONTH_1);
            if (hasMonth1) {
                issueCertificate(policy, CertificateType.ANNUAL_REMAINDER, policy.getStartDate().plusMonths(2), policy.getExpiryDate());
            } else {
                issueCertificate(policy, CertificateType.ANNUAL_FULL, policy.getStartDate(), policy.getExpiryDate());
            }
        }
    }

    private void issueCertificate(Policy policy, CertificateType type, LocalDate start, LocalDate end) {
        log.info("Issuing {} certificate for policy {} ({} to {})", type, policy.getPolicyNumber(), start, end);
        Certificate cert = Certificate.builder()
                .policyId(policy.getId())
                .type(type)
                .startDate(start)
                .expiryDate(end)
                .issuedAt(LocalDateTime.now())
                .build();
        certificateRepository.save(cert);
        // In real world, trigger DMVIC integration here via RabbitMQ
        log.info("Certificate {} saved successfully", type);
    }
}
