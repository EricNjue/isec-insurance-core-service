package com.isec.platform.modules.ocr.infrastructure.extract;

import com.isec.platform.modules.ocr.domain.extraction.ExtractionContext;
import com.isec.platform.modules.ocr.dto.OcrFieldDto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class NtsaSearchRecordExtractor extends BaseNtsaExtractor {

    @Override
    public Map<String, OcrFieldDto> extract(ExtractionContext context) {
        this.results = new HashMap<>();
        extractField(context, "registrationNumber",
                "(VEHICLE\\s*REG(ISTRATION)?\\s*(NO|NUMBER|MARK)|REG(ISTRATION)?\\s*(NO|NUMBER)|REG\\s*)",
                getRegNoPattern(), new BigDecimal("0.95"));
        extractField(context, "chassisNumber",
                "(CHASS?IS\\s*NO|VIN|CHASSIS)",
                getChassisPattern(), new BigDecimal("0.9"));
        extractField(context, "engineNumber",
                "\\b(ENGINE\\s*NO|ENGINE\\s*NUMBER)",
                "[A-Z0-9\\-/]{5,25}", new BigDecimal("0.85"));
        extractField(context, "ownerName",
                "\\b(REGISTERED\\s*OWNER|OWNER\\s*NAME|OWNER)",
                "[A-Z'\\- ]{2,80}", new BigDecimal("0.85"));
        extractField(context, "ownerId",
                "\\b(OWNER\\s*ID|ID\\s*NO|IDENTIFICATION\\s*NO|ID\\s*NUMBER)",
                "\\d{6,12}", new BigDecimal("0.9"));
        extractField(context, "make",
                "(MAKE|MANUFACTURER)",
                "[A-Z][A-Z0-9\\- ]{2,20}", new BigDecimal("0.8"));
        extractField(context, "model",
                "(MODEL)",
                "[A-Z0-9\\- ]{2,25}", new BigDecimal("0.8"));
        extractField(context, "vehicleStatus",
                "(VEHICLE\\s*STATUS|STATUS|CURRENT\\s*STATUS)",
                "[A-Z\\- ]{3,30}", new BigDecimal("0.75"));
        extractField(context, "ntsaSearchReferenceNumber",
                "(SEARCH\\s*REF(?:ERENCE)?\\s*(?:NO|NUMBER)?|REFERENCE\\s*(?:NO|NUMBER)|REF\\s*(?:NO|NUMBER)?)",
                "[A-Z0-9\\-]{5,30}", new BigDecimal("0.85"));
        extractField(context, "searchDate",
                "(SEARCH\\s*DATE|DATE\\s*OF\\s*SEARCH|SEARCH\\s*CONDUCTED\\s*ON|DATE)",
                "\\b\\d{1,2}[/\\-]\\d{1,2}[/\\-](?:19|20)?\\d{2}\\b", new BigDecimal("0.9"));
        // Date of Manufacture (if present)
        extractField(context, "dateOfManufacture",
                "(DATE\\s*OF\\s*MANUFACTURE|DOM|DATE\\s*MANUFACTURED)",
                "\\b\\d{1,2}[/\\-]\\d{1,2}[/\\-](?:19|20)?\\d{2}\\b", new BigDecimal("0.85"));
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
