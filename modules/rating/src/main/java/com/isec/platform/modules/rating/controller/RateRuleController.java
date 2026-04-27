package com.isec.platform.modules.rating.controller;

import com.isec.platform.modules.rating.domain.RateRule;
import com.isec.platform.modules.rating.dto.RateRuleRequest;
import com.isec.platform.modules.rating.service.RateRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rating/rules")
@RequiredArgsConstructor
public class RateRuleController {

    private final RateRuleService rateRuleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')") // RETAIL_USER here implies Tenant Admin role mapping
    public Mono<ResponseEntity<RateRule>> createRule(@Valid @RequestBody RateRuleRequest request) {
        return rateRuleService.createRule(request)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public Mono<ResponseEntity<RateRule>> updateRule(@PathVariable Long id, @Valid @RequestBody RateRuleRequest request) {
        return rateRuleService.updateRule(id, request)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public Mono<ResponseEntity<RateRule>> getRule(@PathVariable Long id) {
        return rateRuleService.getRule(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/book/{rateBookId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public Flux<RateRule> listRules(@PathVariable Long rateBookId) {
        return rateRuleService.listRules(rateBookId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RETAIL_USER')")
    public Mono<ResponseEntity<Void>> deleteRule(@PathVariable Long id) {
        return rateRuleService.deleteRule(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
