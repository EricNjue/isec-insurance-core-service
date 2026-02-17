package com.isec.platform.modules.ocr.infrastructure.extract;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class OcrValidator {

    private static final Pattern PLATE_PATTERN = Pattern.compile("K[A-Z]{1,3}\\s*\\d{3}[A-Z]", Pattern.CASE_INSENSITIVE);
    private static final Pattern VIN_PATTERN = Pattern.compile("[A-Z0-9]{17}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PIN_PATTERN = Pattern.compile("[A-Z][0-9]{9}[A-Z]", Pattern.CASE_INSENSITIVE);
    private static final Pattern ID_PATTERN = Pattern.compile("\\d{6,12}");
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("dd-MM-yy"),
            DateTimeFormatter.ofPattern("d/M/yy")
    };

    public static boolean isValidPlate(String plate) {
        return plate != null && PLATE_PATTERN.matcher(plate.toUpperCase()).matches();
    }

    public static boolean isValidVin(String vin) {
        if (vin == null) return false;
        String v = vin.toUpperCase().replaceAll("\\s+", "");
        if (!VIN_PATTERN.matcher(v).matches()) return false;
        if (v.endsWith("I")) return false;
        return true;
    }

    public static boolean isValidPin(String pin) {
        return pin != null && PIN_PATTERN.matcher(pin.toUpperCase()).matches();
    }

    public static boolean isValidId(String id) {
        return id != null && ID_PATTERN.matcher(id).matches();
    }

    public static boolean isValidDate(String dateStr) {
        if (dateStr == null) return false;
        String clean = dateStr.replace(".", "/").replace(" ", "/");
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate.parse(clean, formatter);
                return true;
            } catch (DateTimeParseException ignore) {}
        }
        return false;
    }

    public static boolean isValidYear(String year) {
        if (year == null) return false;
        try {
            int y = Integer.parseInt(year);
            int currentYear = LocalDate.now().getYear();
            return y > 1900 && y <= currentYear + 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
