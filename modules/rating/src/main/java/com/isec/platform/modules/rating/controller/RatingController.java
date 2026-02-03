package com.isec.platform.modules.rating.controller;

import com.isec.platform.modules.applications.domain.Application;
import com.isec.platform.modules.applications.repository.ApplicationRepository;
import com.isec.platform.modules.rating.dto.QuoteRequest;
import com.isec.platform.modules.rating.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rating")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;
    private final ApplicationRepository applicationRepository;

    @PostMapping("/quote")
    @PreAuthorize("hasAnyRole('RETAIL_USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<RatingService.PremiumBreakdown> getQuote(@RequestBody QuoteRequest request) {
        return applicationRepository.findById(request.getApplicationId())
                .map(app -> ResponseEntity.ok(ratingService.calculatePremium(app.getVehicleValue(), request.getBaseRate())))
                .orElse(ResponseEntity.notFound().build());
    }
}
