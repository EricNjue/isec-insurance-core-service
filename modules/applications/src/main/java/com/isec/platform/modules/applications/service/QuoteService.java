package com.isec.platform.modules.applications.service;

import com.isec.platform.common.multitenancy.TenantContext;
import com.isec.platform.modules.applications.dto.QuoteRequest;
import com.isec.platform.modules.applications.dto.QuoteResponse;
import com.isec.platform.modules.rating.dto.PricingResult;
import com.isec.platform.modules.rating.dto.RatingContext;
import com.isec.platform.modules.rating.service.PricingEngine;
import com.isec.platform.modules.rating.service.RateBookSnapshotLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteService {

    private final PricingEngine pricingEngine;
    private final RateBookSnapshotLoader rateBookSnapshotLoader;
    private final RedisTemplate<String, Object> redisTemplate;

    public QuoteResponse calculateQuote(QuoteRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Calculating quote for tenant: {}, category: {}", tenantId, request.getCategory());

        int vehicleAge = LocalDateTime.now().getYear() - request.getYearOfManufacture();

        RatingContext ratingContext = RatingContext.builder()
                .tenantId(tenantId)
                .category(request.getCategory())
                .vehicleValue(request.getVehicleValue())
                .vehicleAge(vehicleAge)
                .vehicleMake(request.getVehicleMake())
                .vehicleModel(request.getVehicleModel())
                .build();

        PricingResult pricingResult = pricingEngine.price(ratingContext);

        var snapshotOpt = rateBookSnapshotLoader.loadActive(tenantId);
        Long rateBookId = snapshotOpt.map(RateBookSnapshotLoader.Snapshot::rateBookId).orElse(null);
        String rateBookVersion = snapshotOpt.map(RateBookSnapshotLoader.Snapshot::version).orElse(null);
        String cacheKey = snapshotOpt.map(RateBookSnapshotLoader.Snapshot::cacheKey).orElse(null);

        String quoteId = UUID.randomUUID().toString();
        QuoteResponse response = QuoteResponse.builder()
                .quoteId(quoteId)
                .tenantId(tenantId)
                .category(request.getCategory())
                .vehicleMake(request.getVehicleMake())
                .vehicleModel(request.getVehicleModel())
                .yearOfManufacture(request.getYearOfManufacture())
                .vehicleValue(request.getVehicleValue())
                .registrationNumber(request.getRegistrationNumber())
                .chassisNumber(request.getChassisNumber())
                .engineNumber(request.getEngineNumber())
                .rateBookId(rateBookId)
                .rateBookVersion(rateBookVersion)
                .cacheKey(cacheKey)
                .pricing(pricingResult)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .build();

        // Cache quote for application conversion
        redisTemplate.opsForValue().set("quote:" + quoteId, response, Duration.ofDays(30));

        return response;
    }

    public QuoteResponse getQuote(String quoteId) {
        return (QuoteResponse) redisTemplate.opsForValue().get("quote:" + quoteId);
    }
}
