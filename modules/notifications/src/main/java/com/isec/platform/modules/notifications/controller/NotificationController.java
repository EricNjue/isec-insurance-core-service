package com.isec.platform.modules.notifications.controller;

import com.isec.platform.messaging.RabbitMQConfig;
import com.isec.platform.messaging.events.ValuationLetterRequestedEvent;
import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.policies.domain.Policy;
import com.isec.platform.modules.policies.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final RabbitTemplate rabbitTemplate;
    private final PolicyRepository policyRepository;
    private final ApplicationRepository applicationRepository;

    @PostMapping("/reminders/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerReminders() {
        // Logic to trigger expiry reminders via RabbitMQ
        return ResponseEntity.ok("Reminders triggered successfully");
    }

    @PostMapping("/valuation-letter/{policyId}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<String> sendValuationLetter(@PathVariable Long policyId) {
        log.info("Requesting valuation letter for policy: {}", policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));

        Application application = applicationRepository.findById(policy.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application not found for policy: " + policyId));

        ValuationLetterRequestedEvent event = ValuationLetterRequestedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .policyId(policy.getId())
                .policyNumber(policy.getPolicyNumber())
                .registrationNumber(application.getRegistrationNumber())
                .insuredName("Customer Name") // Ideally from user profile or application
                .recipientEmail("customer@example.com") // Should be from user profile
                .correlationId(UUID.randomUUID().toString())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.VALUATION_LETTER_REQUESTED_RK, event);
        
        return ResponseEntity.ok("Valuation letter requested for policy: " + policyId);
    }
}
