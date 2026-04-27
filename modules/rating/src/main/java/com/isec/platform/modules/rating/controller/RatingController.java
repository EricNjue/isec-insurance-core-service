package com.isec.platform.modules.rating.controller;

import com.isec.platform.modules.rating.domain.AnonymousQuote;
import com.isec.platform.modules.rating.dto.AnonymousQuoteRequest;
import com.isec.platform.modules.rating.service.RatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/rating")
@RequiredArgsConstructor
@Slf4j
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/anonymous-quote")
    public Mono<ResponseEntity<AnonymousQuote>> createAnonymousQuote(@RequestBody AnonymousQuoteRequest request) {
        log.info("Received request for anonymous quote");
        return ratingService.createAnonymousQuote(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public Mono<ResponseEntity<RatingService.PremiumBreakdown>> calculatePremium(
            @RequestParam java.math.BigDecimal vehicleValue, 
            @RequestParam java.math.BigDecimal baseRate) {
        return Mono.just(ResponseEntity.ok(ratingService.calculatePremium(vehicleValue, baseRate)));
    }
}
