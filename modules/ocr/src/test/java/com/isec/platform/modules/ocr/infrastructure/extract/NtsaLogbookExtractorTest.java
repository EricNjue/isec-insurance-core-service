package com.isec.platform.modules.ocr.infrastructure.extract;

import com.isec.platform.modules.ocr.domain.extraction.ExtractionContext;
import com.isec.platform.modules.ocr.dto.OcrFieldDto;
import com.isec.platform.modules.ocr.infrastructure.text.TextNormalizer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NtsaLogbookExtractorTest {

    @Test
    void extractsCoreFieldsWithLabelVariations() {
        String text = String.join("\n",
                "National Transport and Safety Authority",
                "VEHICLE REG. KDA 123A",
                "CHASSlS NO: ABCDEFGHJKLMN1234", // note OCR l vs I
                "ENGINE NUMBER: 2KD-FTV12345",
                "MAKE: TOYOTA",
                "MODEL PRADO",
                "BODY TYPE: STATION WAGON",
                "COLOUR: BLACK",
                "FUEL TYPE: DIESEL",
                "YEAR OF MANUFACTURE: 2016",
                "DATE OF MANUFACTURE: 12/08/2015",
                "REGISTERED OWNER: JOHN DOE",
                "ID NO.: 12345678",
                "KRA PIN: A123456789B",
                "REG DATE 01/01/2017",
                "LOGBOOK SERIAL NO: LBK-987654",
                "RATING: A",
                "CONDITION: USED");

        TextNormalizer normalizer = new TextNormalizer();
        String normalized = normalizer.normalize(text);
        Map<String, OcrFieldDto> out = new NtsaLogbookExtractor().extract(
                ExtractionContext.builder()
                        .rawText(text)
                        .normalizedText(normalized)
                        .lines(normalizer.toLines(normalized))
                        .build());

        assertEquals("KDA 123A", out.get("registrationNumber").getValue());
        assertEquals("ABCDEFGHJKLMN1234", out.get("chassisNumber").getValue());
        assertEquals("2KD-FTV12345", out.get("engineNumber").getValue());
        assertEquals("TOYOTA", out.get("make").getValue());
        assertEquals("PRADO", out.get("model").getValue());
        assertEquals("STATION WAGON", out.get("bodyType").getValue());
        assertEquals("BLACK", out.get("color").getValue());
        assertEquals("DIESEL", out.get("fuelType").getValue());
        assertEquals("2016", out.get("yearOfManufacture").getValue());
        assertEquals("12/08/2015", out.get("dateOfManufacture").getValue());
        assertEquals("JOHN DOE", out.get("registeredOwnerName").getValue());
        assertEquals("12345678", out.get("ownerIdNumber").getValue());
        assertEquals("A123456789B", out.get("pinNumber").getValue());
        assertEquals("01/01/2017", out.get("dateOfRegistration").getValue());
        assertEquals("LBK-987654", out.get("logbookSerialNumber").getValue());
        assertEquals("A", out.get("rating").getValue());
        assertEquals("USED", out.get("condition").getValue());
    }

    @Test
    void fallsBackToPatternWhenLabelMissing() {
        String text = String.join(" ",
                "Random paragraph KDA123A more random text",
                "Owner Name JOHN DOE without label",
                "Chassis ABCDEFGHJKLMN1234 present");
        TextNormalizer normalizer = new TextNormalizer();
        String normalized = normalizer.normalize(text);

        Map<String, OcrFieldDto> out = new NtsaLogbookExtractor().extract(
                ExtractionContext.builder()
                        .rawText(text)
                        .normalizedText(normalized)
                        .lines(normalizer.toLines(normalized))
                        .build());

        assertEquals("KDA 123A", out.get("registrationNumber").getValue());
        assertEquals("ABCDEFGHJKLMN1234", out.get("chassisNumber").getValue());
    }
}
