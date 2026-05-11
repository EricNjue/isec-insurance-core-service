package com.isec.platform.modules.applications.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class DateParsingUtil {

    private static final List<DateTimeFormatter> SUPPORTED_DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,                 // yyyy-MM-dd
            DateTimeFormatter.ofPattern("yyyy/MM/dd")         // yyyy/MM/dd
    );

    private DateParsingUtil() {
    }

    /**
     * Parses insurance/canonical dates supporting:
     * - yyyy-MM-dd
     * - yyyy/MM/dd
     * <p>
     * If null/blank, defaults to LocalDate.now().plusDays(1)
     */
    public static LocalDate parseInsuranceStartDate(String value) {

        if (value == null || value.isBlank()) {
            return LocalDate.now().plusDays(1);
        }

        String trimmed = value.trim();

        for (DateTimeFormatter formatter : SUPPORTED_DATE_FORMATS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // try next formatter
            }
        }

        throw new IllegalArgumentException(
                String.format(
                        "Invalid insuranceStartDate format. Expected yyyy-MM-dd or yyyy/MM/dd but got: %s",
                        value
                )
        );
    }
}
