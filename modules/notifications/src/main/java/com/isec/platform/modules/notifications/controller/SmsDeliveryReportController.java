package com.isec.platform.modules.notifications.controller;

import com.isec.platform.modules.notifications.service.DeliveryReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
@Slf4j
public class SmsDeliveryReportController {

    private final DeliveryReportService deliveryReportService;

    @PostMapping(value = "/delivery-report", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> handleForm(@RequestBody MultiValueMap<String, String> form,
                                                          HttpServletRequest request) {
        try {
            Map<String, String> flat = new HashMap<>();
            form.forEach((k, v) -> flat.put(k, v != null && !v.isEmpty() ? v.get(0) : null));
            deliveryReportService.handleFormPayload(flat);
            Map<String, Object> body = Map.of("status", "ok");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.warn("Error processing delivery report: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("status", "received"));
        }
    }

    @PostMapping(value = "/delivery-report", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleJson(@RequestBody Map<String, Object> json,
                                                          HttpServletRequest request) {
        try {
            // Convert to string map for service
            Map<String, String> flat = new HashMap<>();
            json.forEach((k, v) -> flat.put(k, v == null ? null : String.valueOf(v)));
            deliveryReportService.handleFormPayload(flat);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            log.warn("Error processing delivery report JSON: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("status", "received"));
        }
    }
}
