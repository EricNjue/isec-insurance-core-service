package com.isec.platform.modules.applications.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.common.exception.ResourceNotFoundException;
import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.domain.ApplicationStatus;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.policies.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnderwritingService {

    private final ApplicationRepository applicationRepository;
    private final PolicyService policyService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Application approve(Long applicationId, String underwriterId, String comments) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        app.setUnderwriterId(underwriterId);
        app.setUnderwriter_comments(comments);
        app.setStatus(ApplicationStatus.APPROVED_PENDING_PAYMENT);

        // Create policy using the pricing snapshot's total premium if not already created
        BigDecimal totalPremium = extractTotalPremium(app);
        policyService.createPolicy(app.getId(), totalPremium);

        return applicationRepository.save(app);
    }

    @Transactional
    public Application decline(Long applicationId, String underwriterId, String comments, String reason) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        app.setUnderwriterId(underwriterId);
        app.setUnderwriter_comments(comments);
        app.setReferralReason(reason);
        app.setStatus(ApplicationStatus.DECLINED);

        return applicationRepository.save(app);
    }

    private BigDecimal extractTotalPremium(Application app) {
        try {
            if (app.getPricingSnapshot() == null) return null;
            Map<String, Object> map = objectMapper.readValue(app.getPricingSnapshot(), new TypeReference<Map<String, Object>>() {});
            Object total = map.get("totalPremium");
            if (total instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
            if (total instanceof String s) return new BigDecimal(s);
        } catch (Exception e) {
            log.error("Failed to read totalPremium from pricingSnapshot for application {}", app.getId(), e);
        }
        return null;
    }
}
