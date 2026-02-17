package com.isec.platform.modules.ocr.infrastructure.extract;

import com.isec.platform.modules.ocr.domain.extraction.ExtractionContext;
import com.isec.platform.modules.ocr.dto.OcrFieldDto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class NtsaLogbookExtractor extends BaseNtsaExtractor {

    @Override
    public Map<String, OcrFieldDto> extract(ExtractionContext context) {
        this.results = new HashMap<>();
        extractField(context, "registrationNumber",
                "(REG(ISTRATION)?\\s*NO|REG\\.?\\s*NO\\.?|VEHICLE\\s*REG\\.?|REG\\s*)",
                getRegNoPattern(), new BigDecimal("0.95"));
        extractField(context, "chassisNumber",
                "(CHASS?IS\\s*NO|VIN|CHASSIS)",
                getChassisPattern(), new BigDecimal("0.9"));
        extractField(context, "engineNumber",
                "(ENGINE\\s*NO|ENGINE\\s*NUMBER)",
                "[A-Z0-9\\-/]{5,25}", new BigDecimal("0.85"));
        extractField(context, "make",
                "(MAKE|MANUFACTURER)",
                "[A-Z][A-Z0-9\\- ]{2,20}", new BigDecimal("0.8"));
        extractField(context, "model",
                "(MODEL)",
                "[A-Z0-9\\- ]{2,25}", new BigDecimal("0.8"));
        extractField(context, "bodyType",
                "(BODY\\s*TYPE|BODY)",
                "[A-Z\\- ]{3,20}", new BigDecimal("0.75"));
        extractField(context, "color",
                "(COLOUR|COLOR)",
                "[A-Z\\- ]{3,15}", new BigDecimal("0.8"));
        extractField(context, "fuelType",
                "(FUEL\\s*TYPE|FUEL)",
                "(PETROL|DIESEL|ELECTRIC|HYBRID)", new BigDecimal("0.85"));
        extractField(context, "yearOfManufacture",
                "(YEAR\\s*OF\\s*MANUFACTURE|YOM)",
                "(19|20)\\d{2}", new BigDecimal("0.9"));
        // Date of Manufacture (if present)
        extractField(context, "dateOfManufacture",
                "(DATE\\s*OF\\s*MANUFACTURE|DOM|DATE\\s*MANUFACTURED)",
                "\\b\\d{1,2}[/\\-]\\d{1,2}[/\\-](?:19|20)?\\d{2}\\b", new BigDecimal("0.85"));
        extractField(context, "registeredOwnerName",
                "(REGISTERED\\s*OWNER|OWNER\\s*NAME)",
                "[A-Z'\\- ]{3,60}", new BigDecimal("0.85"));
        extractField(context, "ownerIdNumber",
                "(ID\\s*NO|IDENTIFICATION\\s*NO|ID\\s*NUMBER)",
                "\\d{6,12}", new BigDecimal("0.9"));
        extractField(context, "pinNumber",
                "(PIN\\s*NO|KRA\\s*PIN|PIN)",
                "[A-Z][0-9]{9}[A-Z]", new BigDecimal("0.8"));
        extractField(context, "dateOfRegistration",
                "(DATE\\s*OF\\s*REGISTRATION|REG\\.?\\s*DATE)",
                "\\b\\d{1,2}[/\\-]\\d{1,2}[/\\-](?:19|20)?\\d{2}\\b", new BigDecimal("0.9"));
        extractField(context, "logbookSerialNumber",
                "(LOGBOOK\\s*SERIAL\\s*NO|SERIAL\\*NO|SERIAL\\s*NUMBER)",
                "[A-Z0-9\\-]{6,20}", new BigDecimal("0.85"));
        // Rating (if present)
        extractField(context, "rating",
                "(RATING|VEHICLE\\s*RATING|INSPECTION\\s*RATING)",
                "[A-Z0-9\\- ]{1,20}", new BigDecimal("0.7"));
        // Condition (if present)
        extractField(context, "condition",
                "(CONDITION|VEHICLE\\s*CONDITION)",
                "(NEW|USED|EXCELLENT|GOOD|FAIR|POOR)", new BigDecimal("0.7"));

        return results;
    }
}
