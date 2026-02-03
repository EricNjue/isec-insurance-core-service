package com.isec.platform.modules.rating.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RatingService {

    private static final BigDecimal PCF_RATE = new BigDecimal("0.0025"); // 0.25%
    private static final BigDecimal ITL_RATE = new BigDecimal("0.0020"); // 0.20%
    private static final BigDecimal CERT_CHARGE = new BigDecimal("40.00");

    public PremiumBreakdown calculatePremium(BigDecimal vehicleValue, BigDecimal baseRate) {
        BigDecimal basePremium = vehicleValue.multiply(baseRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pcf = basePremium.multiply(PCF_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal itl = basePremium.multiply(ITL_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = basePremium.add(pcf).add(itl).add(CERT_CHARGE);

        return new PremiumBreakdown(basePremium, pcf, itl, CERT_CHARGE, total);
    }

    public record PremiumBreakdown(
        BigDecimal basePremium,
        BigDecimal pcf,
        BigDecimal itl,
        BigDecimal certCharge,
        BigDecimal totalPremium
    ) {}
}
