package com.isec.platform.modules.rating.controller;

import com.isec.platform.modules.rating.domain.RateRule;
import com.isec.platform.modules.rating.dto.RateRuleRequest;
import com.isec.platform.modules.rating.service.RateRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rating/rules")
@RequiredArgsConstructor
public class RateRuleController {

    private final RateRuleService rateRuleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')") // RETAIL_USER here implies Tenant Admin role mapping
    public ResponseEntity<RateRule> createRule(@Valid @RequestBody RateRuleRequest request) {
        return ResponseEntity.ok(rateRuleService.createRule(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public ResponseEntity<RateRule> updateRule(@PathVariable Long id, @Valid @RequestBody RateRuleRequest request) {
        return ResponseEntity.ok(rateRuleService.updateRule(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public ResponseEntity<RateRule> getRule(@PathVariable Long id) {
        return ResponseEntity.ok(rateRuleService.getRule(id));
    }

    @GetMapping("/book/{rateBookId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public ResponseEntity<List<RateRule>> listRules(@PathVariable Long rateBookId) {
        return ResponseEntity.ok(rateRuleService.listRules(rateBookId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        rateRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
