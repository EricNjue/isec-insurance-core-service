package com.isec.platform.modules.certificates.service;

import com.isec.platform.messaging.RabbitMQConfig;
import com.isec.platform.messaging.events.CertificateRequestedEvent;
import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.certificates.domain.Certificate;
import com.isec.platform.modules.certificates.domain.CertificateStatus;
import com.isec.platform.modules.certificates.domain.CertificateType;
import com.isec.platform.modules.certificates.repository.CertificateRepository;
import com.isec.platform.modules.policies.domain.Policy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final ApplicationRepository applicationRepository;
    private final RabbitTemplate rabbitTemplate;
    private static final BigDecimal MONTHLY_PREMIUM_RATE = new BigDecimal("0.35");

    @Transactional
    public void processCertificateIssuance(Policy policy, BigDecimal amountPaid, String recipientEmail, String recipientPhoneNumber) {
        log.info("Processing certificate issuance for policy: {}, amount paid: {}, recipient: {}", policy.getPolicyNumber(), amountPaid, recipientEmail);
        
        BigDecimal annualPremium = policy.getTotalAnnualPremium();
        BigDecimal monthlyRequirement = annualPremium.multiply(MONTHLY_PREMIUM_RATE).setScale(2, RoundingMode.HALF_UP);
        
        List<Certificate> issued = certificateRepository.findByPolicyId(policy.getId());
        
        Application application = applicationRepository.findById(policy.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application not found for policy: " + policy.getId()));

        // Month 1 & 2 logic
        if (amountPaid.compareTo(monthlyRequirement) >= 0 && issued.isEmpty()) {
            log.info("Policy {} qualified for Month 1 certificate", policy.getPolicyNumber());
            requestCertificate(policy, application, CertificateType.MONTH_1, policy.getStartDate(), policy.getStartDate().plusMonths(1).minusDays(1), recipientEmail, recipientPhoneNumber);
            
            // Check if they paid enough for Month 2 as well
            if (amountPaid.compareTo(monthlyRequirement.multiply(new BigDecimal("2"))) >= 0) {
                log.info("Policy {} qualified for Month 2 certificate", policy.getPolicyNumber());
                requestCertificate(policy, application, CertificateType.MONTH_2, policy.getStartDate().plusMonths(1), policy.getStartDate().plusMonths(2).minusDays(1), recipientEmail, recipientPhoneNumber);
            }
        }
        
        // Month 3 (Annual Remainder) logic - Only if balance is zero
        if (policy.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Policy {} is fully paid. Requesting final certificates", policy.getPolicyNumber());
            boolean hasMonth1 = issued.stream().anyMatch(c -> c.getType() == CertificateType.MONTH_1);
            if (hasMonth1) {
                requestCertificate(policy, application, CertificateType.ANNUAL_REMAINDER, policy.getStartDate().plusMonths(2), policy.getExpiryDate(), recipientEmail, recipientPhoneNumber);
            } else {
                requestCertificate(policy, application, CertificateType.ANNUAL_FULL, policy.getStartDate(), policy.getExpiryDate(), recipientEmail, recipientPhoneNumber);
            }
        }
    }

    private void requestCertificate(Policy policy, Application application, CertificateType type, LocalDate start, LocalDate end, String recipientEmail, String recipientPhoneNumber) {
        log.info("Creating pending {} certificate for policy {}", type, policy.getPolicyNumber());
        
        String idempotencyKey = UUID.randomUUID().toString();
        
        Certificate certificate = Certificate.builder()
                .policyId(policy.getId())
                .type(type)
                .status(CertificateStatus.PENDING)
                .startDate(start)
                .expiryDate(end)
                .idempotencyKey(idempotencyKey)
                .build();
        
        certificateRepository.save(certificate);
        
        CertificateRequestedEvent event = CertificateRequestedEvent.builder()
                .eventId(idempotencyKey)
                .policyId(policy.getId())
                .policyNumber(policy.getPolicyNumber())
                .registrationNumber(application.getRegistrationNumber())
                .certificateType(type.name())
                .startDate(start)
                .expiryDate(end)
                .recipientEmail(recipientEmail)
                .recipientPhoneNumber(recipientPhoneNumber)
                .correlationId(UUID.randomUUID().toString())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.CERTIFICATE_REQUESTED_RK, event);
        log.info("Certificate request event sent for type: {} with eventId: {}", type, event.getEventId());
    }
}
