package com.isec.platform.modules.notifications.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    @PostMapping("/reminders/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerReminders() {
        // Logic to trigger expiry reminders via RabbitMQ
        return ResponseEntity.ok("Reminders triggered successfully");
    }

    @PostMapping("/valuation-letter/{policyId}")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<String> sendValuationLetter(@PathVariable Long policyId) {
        // Logic to generate and send valuation letter
        return ResponseEntity.ok("Valuation letter requested for policy: " + policyId);
    }
}
