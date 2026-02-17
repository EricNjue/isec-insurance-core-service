package com.isec.platform.modules.ocr.infrastructure.extract;

import com.isec.platform.modules.ocr.dto.OcrFieldDto;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NtsaLogbookExtractorTest {

    @Test
    void extractsCoreFieldsWithLabelVariations() {
        String text = String.join("\n",
                "National Transport and Safety Authority",
                "VEHICLE REG. KDA 123A",
                "CHASSlS NO: ABCDEFGHJKLMN123", // note OCR l vs I
                "ENGINE NUMBER: 2KD-FTV12345",
                "MAKE: TOYOTA",
                "MODEL PRADO",
                "BODY TYPE: STATION WAGON",
                "COLOUR: BLACK",
                "FUEL TYPE: DIESEL",
                "YEAR OF MANUFACTURE: 2016",
                "REGISTERED OWNER: JOHN DOE",
                "ID NO.: 12345678",
                "KRA PIN: A123456789B",
                "REG DATE 01/01/2017",
                "LOGBOOK SERIAL NO: LBK-987654");

        BaseNtsaExtractor extractor = new NtsaLogbookExtractor();
        Map<String, OcrFieldDto> out = extractor.extract(text.toUpperCase());

        assertEquals("KDA 123A", out.get("registrationNumber").getValue());
        assertEquals("ABCDEFGHJKLMN123", out.get("chassisNumber").getValue());
        assertEquals("2KD-FTV12345", out.get("engineNumber").getValue());
        assertEquals("TOYOTA", out.get("make").getValue());
        assertEquals("PRADO", out.get("model").getValue());
        assertEquals("STATION WAGON", out.get("bodyType").getValue());
        assertEquals("BLACK", out.get("color").getValue());
        assertEquals("DIESEL", out.get("fuelType").getValue());
        assertEquals("2016", out.get("yearOfManufacture").getValue());
        assertEquals("JOHN DOE", out.get("registeredOwnerName").getValue());
        assertEquals("12345678", out.get("ownerIdNumber").getValue());
        assertEquals("A123456789B", out.get("pinNumber").getValue());
        assertEquals("01/01/2017", out.get("dateOfRegistration").getValue());
        assertEquals("LBK-987654", out.get("logbookSerialNumber").getValue());
    }

    @Test
    void fallsBackToPatternWhenLabelMissing() {
        String text = String.join(" ",
                "Random paragraph KDA123A more random text",
                "Owner Name JOHN DOE without label",
                "Chassis ABCDEFGHJKLMN123 present");
        BaseNtsaExtractor extractor = new NtsaLogbookExtractor();
        Map<String, OcrFieldDto> out = extractor.extract(text.toUpperCase());

        assertEquals("KDA123A", out.get("registrationNumber").getValue());
        assertEquals("ABCDEFGHJKLMN123", out.get("chassisNumber").getValue());
    }
}
