package com.isec.platform.modules.ocr.infrastructure.extract;

import com.isec.platform.modules.ocr.dto.OcrFieldDto;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NtsaSearchRecordExtractorTest {

    @Test
    void extractsSearchRecordFields() {
        String text = String.join("\n",
                "Motor Vehicle Search Record",
                "Search Ref: NSR-2024-00001",
                "Reg No KDA123A",
                "Chassis: ABCDEFGHJKLMN123",
                "Engine No: 2KD1234567",
                "Owner: JANE DOE",
                "Owner ID 23456789",
                "Make TOYOTA",
                "Model RAV4",
                "Vehicle Status: ACTIVE",
                "Search Date: 02/02/2024");

        BaseNtsaExtractor extractor = new NtsaSearchRecordExtractor();
        Map<String, OcrFieldDto> out = extractor.extract(text.toUpperCase());

        assertEquals("KDA123A", out.get("registrationNumber").getValue());
        assertEquals("ABCDEFGHJKLMN123", out.get("chassisNumber").getValue());
        assertEquals("2KD1234567", out.get("engineNumber").getValue());
        assertEquals("JANE DOE", out.get("ownerName").getValue());
        assertEquals("23456789", out.get("ownerId").getValue());
        assertEquals("TOYOTA", out.get("make").getValue());
        assertEquals("RAV4", out.get("model").getValue());
        assertEquals("ACTIVE", out.get("vehicleStatus").getValue());
        assertEquals("NSR-2024-00001", out.get("ntsaSearchReferenceNumber").getValue());
        assertEquals("02/02/2024", out.get("searchDate").getValue());
    }
}
