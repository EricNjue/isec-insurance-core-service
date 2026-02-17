package com.isec.platform.modules.ocr.infrastructure.extract;

import com.isec.platform.modules.ocr.dto.OcrFieldDto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class NtsaSearchRecordExtractor extends BaseNtsaExtractor {

    @Override
    public Map<String, OcrFieldDto> extract(String fullText) {
        this.results = new HashMap<>();
        // Registration Number
        extractField(fullText, "registrationNumber",
                "(REG(ISTRATION)?\\s*NO|REG\\.?\\s*NO\\.?|VEHICLE\\s*REG\\.?|REG\\s*)",
                getRegNoPattern(), new BigDecimal("0.95"));
        // Chassis Number (VIN)
        extractField(fullText, "chassisNumber",
                "(CHASS?IS\\s*NO|VIN|CHASSIS)",
                getChassisPattern(), new BigDecimal("0.9"));
        // Engine Number
        extractField(fullText, "engineNumber",
                "\\b(ENGINE\\s*NO|ENGINE\\s*NUMBER)",
                "[A-Z0-9\\-]{5,20}", new BigDecimal("0.85"));
        // Owner Name
        extractField(fullText, "ownerName",
                "\\b(REGISTERED\\s*OWNER|OWNER\\s*NAME|OWNER)",
                "[A-Z'\\- ]{3,60}", new BigDecimal("0.85"));
        // Owner ID
        extractField(fullText, "ownerId",
                "\\b(OWNER\\s*ID|ID\\s*NO|IDENTIFICATION\\s*NO|ID\\s*NUMBER)",
                "\\d{6,12}", new BigDecimal("0.9"));
        // Make
        extractField(fullText, "make",
                "(MAKE|MANUFACTURER)",
                "[A-Z][A-Z0-9\\- ]{2,20}", new BigDecimal("0.8"));
        // Model
        extractField(fullText, "model",
                "(MODEL)",
                "[A-Z0-9\\- ]{2,25}", new BigDecimal("0.8"));
        // Vehicle Status
        extractField(fullText, "vehicleStatus",
                "(VEHICLE\\s*STATUS|STATUS)",
                "[A-Z\\- ]{3,25}", new BigDecimal("0.75"));
        // NTSA Search Reference Number
        extractField(fullText, "ntsaSearchReferenceNumber",
                "(SEARCH\\s*REF(?:ERENCE)?\\s*NO|SEARCH\\s*REF)",
                "[A-Z0-9\\-]{6,20}", new BigDecimal("0.85"));
        // Search Date
        extractField(fullText, "searchDate",
                "(SEARCH\\s*DATE|DATE\\s*OF\\s*SEARCH|DATE)",
                "\\b\\d{1,2}[/\\-]\\d{1,2}[/\\-](?:19|20)?\\d{2}\\b", new BigDecimal("0.9"));

        return results;
    }
}
