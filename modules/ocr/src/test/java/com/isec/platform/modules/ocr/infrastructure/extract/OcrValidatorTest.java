package com.isec.platform.modules.ocr.infrastructure.extract;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OcrValidatorTest {

    @Test
    void testIsValidPlate() {
        assertTrue(OcrValidator.isValidPlate("KDA 123A"));
        assertTrue(OcrValidator.isValidPlate("KDA123A"));
        assertTrue(OcrValidator.isValidPlate("KBA 001Z"));
        assertFalse(OcrValidator.isValidPlate("ABC 123"));
        assertFalse(OcrValidator.isValidPlate("123 KDA"));
    }

    @Test
    void testIsValidVin() {
        assertTrue(OcrValidator.isValidVin("1ABC2DEF3GHI4JKL5"));
        assertFalse(OcrValidator.isValidVin("1ABC2DEF3GHI4JKL"));
        assertFalse(OcrValidator.isValidVin("1ABC2DEF3GHI4JKL56"));
        assertFalse(OcrValidator.isValidVin("1ABC2DEF3GHI4JKLI"));
    }

    @Test
    void testIsValidPin() {
        assertTrue(OcrValidator.isValidPin("A123456789B"));
        assertFalse(OcrValidator.isValidPin("1234567890"));
        assertFalse(OcrValidator.isValidPin("AB12345678C"));
    }

    @Test
    void testIsValidId() {
        assertTrue(OcrValidator.isValidId("12345678"));
        assertTrue(OcrValidator.isValidId("123456"));
        assertTrue(OcrValidator.isValidId("123456789012"));
        assertFalse(OcrValidator.isValidId("12345"));
        assertFalse(OcrValidator.isValidId("ABC12345"));
    }

    @Test
    void testIsValidDate() {
        assertTrue(OcrValidator.isValidDate("01/01/2023"));
        assertTrue(OcrValidator.isValidDate("01-01-2023"));
        assertTrue(OcrValidator.isValidDate("2023-01-01"));
        assertTrue(OcrValidator.isValidDate("1/1/2023"));
        assertTrue(OcrValidator.isValidDate("01.01.2023"));
        assertTrue(OcrValidator.isValidDate("01/01/23"));
        assertFalse(OcrValidator.isValidDate("2023/13/01"));
        assertFalse(OcrValidator.isValidDate("not-a-date"));
    }

    @Test
    void testIsValidYear() {
        assertTrue(OcrValidator.isValidYear("2020"));
        assertTrue(OcrValidator.isValidYear("1995"));
        assertFalse(OcrValidator.isValidYear("1899"));
        assertFalse(OcrValidator.isValidYear("2100"));
        assertFalse(OcrValidator.isValidYear("ABCD"));
    }
}
