package com.isec.platform.modules.rating.service;

import com.isec.platform.modules.rating.domain.AnonymousQuote;
import com.isec.platform.modules.rating.dto.AnonymousQuoteRequest;
import com.isec.platform.modules.rating.repository.AnonymousQuoteRepository;
import com.isec.platform.modules.rating.dto.PricingResult;
import com.isec.platform.modules.rating.dto.RatingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final AnonymousQuoteRepository anonymousQuoteRepository;
    private final PricingEngine pricingEngine;

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

    public AnonymousQuote createAnonymousQuote(AnonymousQuoteRequest request) {
        log.info("Creating anonymous quote for vehicle: {} {}", request.getVehicleMake(), request.getVehicleModel());

        String tenantId = request.getTenantId() != null ? request.getTenantId() : "SANLAM";
        String category = request.getCategory() != null ? request.getCategory() : "PRIVATE_CAR";

        RatingContext context = RatingContext.builder()
                .tenantId(tenantId)
                .category(category)
                .vehicleValue(request.getVehicleValue())
                .vehicleMake(request.getVehicleMake())
                .vehicleModel(request.getVehicleModel())
                .vehicleAge(calculateVehicleAge(request.getYearOfManufacture()))
                .build();

        PricingResult pricingResult = pricingEngine.price(context);

        PremiumBreakdown breakdown = new PremiumBreakdown(
                pricingResult.getBasePremium(),
                pricingResult.getPcf(),
                pricingResult.getItl(),
                pricingResult.getCertificateCharge(),
                pricingResult.getTotalPremium()
        );

        AnonymousQuote quote = AnonymousQuote.builder()
                .id(UUID.randomUUID().toString())
                .vehicleMake(request.getVehicleMake())
                .vehicleModel(request.getVehicleModel())
                .yearOfManufacture(request.getYearOfManufacture())
                .vehicleValue(request.getVehicleValue())
                .baseRate(request.getBaseRate())
                .premiumBreakdown(breakdown)
                .build();

        return anonymousQuoteRepository.save(quote);
    }

    private Integer calculateVehicleAge(Integer yearOfManufacture) {
        if (yearOfManufacture == null) return null;
        return LocalDate.now().getYear() - yearOfManufacture;
    }

    public Optional<AnonymousQuote> getAnonymousQuote(String id) {
        return anonymousQuoteRepository.findById(id);
    }

    public record PremiumBreakdown(
        BigDecimal basePremium,
        BigDecimal pcf,
        BigDecimal itl,
        BigDecimal certCharge,
        BigDecimal totalPremium
    ) {}
}
