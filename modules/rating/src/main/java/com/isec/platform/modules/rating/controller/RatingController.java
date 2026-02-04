package com.isec.platform.modules.rating.controller;

import com.isec.platform.modules.rating.domain.AnonymousQuote;
import com.isec.platform.modules.rating.dto.AnonymousQuoteRequest;
import com.isec.platform.modules.rating.service.RatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rating")
@RequiredArgsConstructor
@Slf4j
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/anonymous-quote")
    public ResponseEntity<AnonymousQuote> createAnonymousQuote(@RequestBody AnonymousQuoteRequest request) {
        log.info("Received request for anonymous quote");
        return ResponseEntity.ok(ratingService.createAnonymousQuote(request));
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<RatingService.PremiumBreakdown> calculatePremium(
            @RequestParam java.math.BigDecimal vehicleValue, 
            @RequestParam java.math.BigDecimal baseRate) {
        return ResponseEntity.ok(ratingService.calculatePremium(vehicleValue, baseRate));
    }
}
