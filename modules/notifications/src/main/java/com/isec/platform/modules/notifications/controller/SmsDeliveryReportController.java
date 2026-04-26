package com.isec.platform.modules.notifications.controller;

import com.isec.platform.modules.notifications.service.DeliveryReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
@Slf4j
public class SmsDeliveryReportController {

    private final DeliveryReportService deliveryReportService;

    @PostMapping(value = "/delivery-report", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> handleForm(ServerWebExchange exchange) {
        return exchange.getFormData().flatMap(form -> {
            Map<String, String> flat = new HashMap<>();
            form.forEach((k, v) -> flat.put(k, v != null && !v.isEmpty() ? v.get(0) : null));
            return deliveryReportService.handleFormPayload(flat);
        })
                .then(Mono.just(ResponseEntity.ok(Map.of("status", (Object)"ok"))))
                .onErrorResume(e -> {
                    log.warn("Error processing delivery report: {}", e.getMessage());
                    return Mono.just(ResponseEntity.ok(Map.of("status", (Object)"received")));
                });
    }

    @PostMapping(value = "/delivery-report", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> handleJson(@RequestBody Map<String, Object> json) {
        Map<String, String> flat = new HashMap<>();
        json.forEach((k, v) -> flat.put(k, v == null ? null : String.valueOf(v)));
        
        return deliveryReportService.handleFormPayload(flat)
                .then(Mono.just(ResponseEntity.ok(Map.of("status", (Object)"ok"))))
                .onErrorResume(e -> {
                    log.warn("Error processing delivery report JSON: {}", e.getMessage());
                    return Mono.just(ResponseEntity.ok(Map.of("status", (Object)"received")));
                });
    }
}
