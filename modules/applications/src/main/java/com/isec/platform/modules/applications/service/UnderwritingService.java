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
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnderwritingService {

    private final ApplicationRepository applicationRepository;
    private final PolicyService policyService;
    private final ObjectMapper objectMapper;

    public Mono<Application> approve(Long applicationId, String underwriterId, String comments) {
        return applicationRepository.findById(applicationId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Application", applicationId)))
                .flatMap(app -> {
                    app.setUnderwriterId(underwriterId);
                    app.setUnderwriter_comments(comments);
                    app.setStatus(ApplicationStatus.APPROVED_PENDING_PAYMENT);

                    BigDecimal totalPremium = extractTotalPremium(app);
                    return policyService.createPolicy(app.getId(), totalPremium)
                            .flatMap(policy -> applicationRepository.save(app));
                });
    }

    public Mono<Application> decline(Long applicationId, String underwriterId, String comments, String reason) {
        return applicationRepository.findById(applicationId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Application", applicationId)))
                .flatMap(app -> {
                    app.setUnderwriterId(underwriterId);
                    app.setUnderwriter_comments(comments);
                    app.setReferralReason(reason);
                    app.setStatus(ApplicationStatus.DECLINED);

                    return applicationRepository.save(app);
                });
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
