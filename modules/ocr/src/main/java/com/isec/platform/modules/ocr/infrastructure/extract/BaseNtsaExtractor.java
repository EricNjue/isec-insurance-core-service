package com.isec.platform.modules.ocr.infrastructure.extract;

import com.isec.platform.modules.ocr.domain.extraction.ExtractionContext;
import com.isec.platform.modules.ocr.dto.OcrFieldDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseNtsaExtractor implements DocumentExtractor {

    protected Map<String, OcrFieldDto> results = new HashMap<>();

    protected void extractField(ExtractionContext context, String fieldName, String labelPattern,
                                String valuePattern, BigDecimal defaultConfidence) {
        String fullText = context.getNormalizedText();
        List<String> lines = context.getLines();
        if ((lines == null || lines.isEmpty()) && fullText != null && !fullText.isBlank()) {
            lines = java.util.List.of(fullText);
        }

        Pattern label = Pattern.compile(labelPattern, Pattern.CASE_INSENSITIVE);
        Pattern value = Pattern.compile(valuePattern, Pattern.CASE_INSENSITIVE);

        // 1) Line-aware extraction: label + same line or next lines
        ExtractionCandidate best = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher lm = label.matcher(line);
            if (!lm.find()) continue;

            ExtractionCandidate candidate = extractFromLine(line, lm, value, fieldName, defaultConfidence);
            if (candidate != null) {
                best = candidate;
                break; // prefer closest match
            }

            // Try next line if value likely on following line
            if (i + 1 < lines.size()) {
                String nextLine = lines.get(i + 1);
                candidate = extractFromBlock(line + " " + nextLine, value, fieldName, defaultConfidence, 0.9);
                if (candidate != null) {
                    best = candidate;
                    break;
                }
            }
        }

        if (best == null && fullText != null && !fullText.isBlank()) {
            // 2) Fallback: value pattern in whole text
            ExtractionCandidate candidate = extractFromBlock(fullText, value, fieldName, defaultConfidence, 0.7);
            if (candidate != null) {
                best = candidate;
            }
        }

        if (best == null) {
            results.put(fieldName, OcrFieldDto.builder()
                    .value(null)
                    .confidence(BigDecimal.ZERO)
                    .status("NOT_FOUND")
                    .build());
            return;
        }

        results.put(fieldName, OcrFieldDto.builder()
                .value(best.value)
                .confidence(best.confidence)
                .status(best.status)
                .build());
    }

    private ExtractionCandidate extractFromLine(String line, Matcher labelMatcher, Pattern value,
                                                String fieldName, BigDecimal defaultConfidence) {
        int end = labelMatcher.end();
        String after = line.substring(end).trim();
        if (after.startsWith(":")) after = after.substring(1).trim();
        ExtractionCandidate candidate = extractFromBlock(after, value, fieldName, defaultConfidence, 1.0);
        if (candidate != null) return candidate;

        // Sometimes label and value are separated by extra tokens, try the whole line
        return extractFromBlock(line, value, fieldName, defaultConfidence, 0.95);
    }

    private ExtractionCandidate extractFromBlock(String block, Pattern valuePattern,
                                                 String fieldName, BigDecimal defaultConfidence, double factor) {
        Matcher vm = valuePattern.matcher(block);
        while (vm.find()) {
            String val = vm.group().trim();
            if (isPartialNumericMatch(block, vm.start(), vm.end(), valuePattern)) {
                continue;
            }
            String normalized = normalizeValue(fieldName, val);

            BigDecimal confidence = defaultConfidence.multiply(BigDecimal.valueOf(factor))
                    .setScale(4, RoundingMode.HALF_UP);
            String status = "EXTRACTED";

            if (!isValid(fieldName, normalized)) {
                confidence = confidence.multiply(new BigDecimal("0.5")).setScale(4, RoundingMode.HALF_UP);
                status = "LOW_CONFIDENCE";
            }

            return new ExtractionCandidate(normalized, confidence, status);
        }
        return null;
    }

    private boolean isPartialNumericMatch(String block, int start, int end, Pattern valuePattern) {
        String pattern = valuePattern.pattern();
        boolean numeric = pattern.contains("\\d") || pattern.contains("0-9");
        if (!numeric) return false;
        boolean digitBefore = start > 0 && Character.isDigit(block.charAt(start - 1));
        boolean digitAfter = end < block.length() && Character.isDigit(block.charAt(end));
        return digitBefore || digitAfter;
    }

    private String normalizeValue(String fieldName, String value) {
        if (value == null) return null;
        String v = value.trim();

        if ("registrationNumber".equals(fieldName)) {
            v = v.replaceAll("\\s+", "");
            java.util.regex.Matcher rm = java.util.regex.Pattern.compile("^(K[A-Z0-9]{1,3})([A-Z0-9]{4})$").matcher(v);
            if (rm.find()) {
                String prefix = rm.group(1).replace('0', 'O').replace('1', 'I');
                String suffix = rm.group(2);
                String digits = suffix.substring(0, 3).replace('O', '0').replace('I', '1').replace('L', '1');
                String last = suffix.substring(3).replace('0', 'O').replace('1', 'I');
                v = prefix + digits + last;
            }
            if (v.matches("K[A-Z]{1,3}\\d{3}[A-Z]")) {
                v = v.replaceAll("(K[A-Z]{1,3})(\\d{3}[A-Z])", "$1 $2");
            }
            return v;
        }

        if ("chassisNumber".equals(fieldName)) {
            v = v.replaceAll("\\s+", "");
            v = v.replace('O', '0').replace('Q', '0').replace('I', '1');
            return v;
        }

        if ("engineNumber".equals(fieldName) || "logbookSerialNumber".equals(fieldName)) {
            return v.replaceAll("\\s+", "");
        }

        if ("ownerIdNumber".equals(fieldName) || "ownerId".equals(fieldName) || "yearOfManufacture".equals(fieldName)) {
            return v.replace('O', '0').replace('I', '1').replace('L', '1').replace('S', '5');
        }

        if ("pinNumber".equals(fieldName)) {
            return v.replace('O', '0').replace('I', '1').replace('L', '1');
        }

        if ("dateOfRegistration".equals(fieldName) || "searchDate".equals(fieldName)) {
            v = v.replace('.', '/').replace(' ', '/');
            return v;
        }

        return v;
    }

    private boolean isValid(String fieldName, String value) {
        if (value == null || value.isBlank()) return false;
        return switch (fieldName) {
            case "registrationNumber" -> OcrValidator.isValidPlate(value);
            case "chassisNumber" -> OcrValidator.isValidVin(value);
            case "pinNumber" -> OcrValidator.isValidPin(value);
            case "ownerIdNumber", "ownerId" -> OcrValidator.isValidId(value);
            case "dateOfRegistration", "searchDate", "dateOfManufacture" -> OcrValidator.isValidDate(value);
            case "yearOfManufacture" -> OcrValidator.isValidYear(value);
            default -> true;
        };
    }

    protected String getRegNoPattern() {
        return "K[A-Z]{1,3}\\s*\\d{3}[A-Z]";
    }

    protected String getChassisPattern() {
        return "[A-Z0-9]{17}";
    }

    private record ExtractionCandidate(String value, BigDecimal confidence, String status) {}
}
