package com.isec.platform.modules.rating.domain;

import com.isec.platform.modules.rating.service.RatingService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import java.math.BigDecimal;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "AnonymousQuote", timeToLive = 3600) // 1 hour TTL
public class AnonymousQuote implements Serializable {
    @Id
    private String id;
    private String vehicleMake;
    private String vehicleModel;
    private Integer yearOfManufacture;
    private BigDecimal vehicleValue;
    private BigDecimal baseRate;
    private RatingService.PremiumBreakdown premiumBreakdown;
}
