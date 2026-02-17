package com.isec.platform.modules.ocr.infrastructure.extract;

import com.isec.platform.modules.ocr.dto.OcrFieldDto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class NtsaLogbookExtractor extends BaseNtsaExtractor {

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
                "(ENGINE\\s*NO|ENGINE\\s*NUMBER)",
                "[A-Z0-9\\-]{5,20}", new BigDecimal("0.85"));
        // Make
        extractField(fullText, "make",
                "(MAKE|MANUFACTURER)",
                "[A-Z][A-Z0-9\\- ]{2,20}", new BigDecimal("0.8"));
        // Model
        extractField(fullText, "model",
                "(MODEL)",
                "[A-Z0-9\\- ]{2,25}", new BigDecimal("0.8"));
        // Body Type
        extractField(fullText, "bodyType",
                "(BODY\\s*TYPE|BODY)",
                "[A-Z\\- ]{3,20}", new BigDecimal("0.75"));
        // Color
        extractField(fullText, "color",
                "(COLOUR|COLOR)",
                "[A-Z\\- ]{3,15}", new BigDecimal("0.8"));
        // Fuel Type
        extractField(fullText, "fuelType",
                "(FUEL\\s*TYPE|FUEL)",
                "(PETROL|DIESEL|ELECTRIC|HYBRID)", new BigDecimal("0.85"));
        // Year of Manufacture
        extractField(fullText, "yearOfManufacture",
                "(YEAR\\s*OF\\s*MANUFACTURE|YOM)",
                "(19|20)\\d{2}", new BigDecimal("0.9"));
        // Registered Owner Name
        extractField(fullText, "registeredOwnerName",
                "(REGISTERED\\s*OWNER|OWNER\\s*NAME)",
                "[A-Z'\\- ]{3,60}", new BigDecimal("0.85"));
        // Owner ID Number
        extractField(fullText, "ownerIdNumber",
                "(ID\\s*NO|IDENTIFICATION\\s*NO|ID\\s*NUMBER)",
                "\\d{6,12}", new BigDecimal("0.9"));
        // PIN Number (if present)
        extractField(fullText, "pinNumber",
                "(PIN\\s*NO|KRA\\s*PIN|PIN)",
                "[A-Z][0-9]{9}[A-Z]", new BigDecimal("0.8"));
        // Date of Registration
        extractField(fullText, "dateOfRegistration",
                "(DATE\\s*OF\\s*REGISTRATION|REG\\.?\\s*DATE)",
                "\\b\\d{1,2}[/\\-]\\d{1,2}[/\\-](?:19|20)?\\d{2}\\b", new BigDecimal("0.9"));
        // Logbook Serial Number
        extractField(fullText, "logbookSerialNumber",
                "(LOGBOOK\\s*SERIAL\\s*NO|SERIAL\\*NO|SERIAL\\s*NUMBER)",
                "[A-Z0-9\\-]{6,20}", new BigDecimal("0.85"));

        return results;
    }
}
