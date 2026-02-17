package com.isec.platform.modules.ocr.infrastructure.extract;

import com.isec.platform.modules.ocr.dto.OcrFieldDto;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseNtsaExtractor implements DocumentExtractor {

    protected Map<String, OcrFieldDto> results = new HashMap<>();

    protected void extractField(String fullText, String fieldName, String labelPattern, String valuePattern, BigDecimal defaultConfidence) {
        // Layered strategy: 
        // 1. Label-based search with proximity
        // 2. Pattern-based if label failed or to confirm
        
        // Find label
        Pattern lp = Pattern.compile("(?si)" + labelPattern);
        Matcher lm = lp.matcher(fullText);
        
        // Use a list to store possible label matches and try them in order
        lm.reset();
        while (lm.find()) {
            int end = lm.end();
            // Look for value pattern after label within 120 chars
            String sub = fullText.substring(end, Math.min(end + 120, fullText.length()));
            
            // Modified value pattern to ensure it's not preceded by a digit if it's a number
            String vpStr = "(?si).*?(" + valuePattern + ")\\b";
            Pattern vp = Pattern.compile(vpStr);
            Matcher vm = vp.matcher(sub);
            
            if (vm.find()) {
                String val = vm.group(1).trim();
                
                // Extra check: if we are looking for a numeric field but we matched part of a longer digit string, skip it
                if (valuePattern.contains("\\d") || valuePattern.contains("0-9")) {
                    int startIdx = vm.start(1);
                    int endIdx = vm.end(1);
                    
                    boolean digitBefore = (startIdx > 0 && Character.isDigit(sub.charAt(startIdx - 1)));
                    boolean digitAfter = (endIdx < sub.length() && Character.isDigit(sub.charAt(endIdx)));
                    
                    if (digitBefore || digitAfter) {
                        continue; // matched part of a larger number, try next label match
                    }
                }

                BigDecimal confidence = defaultConfidence;
                String status = "EXTRACTED";
                
                // Validate field and adjust confidence/status
                if (!isValid(fieldName, val)) {
                    confidence = confidence.multiply(new BigDecimal("0.5"));
                    status = "LOW_CONFIDENCE";
                }
                
                results.put(fieldName, OcrFieldDto.builder()
                        .value(val)
                        .confidence(confidence)
                        .status(status)
                        .build());
                return;
            }
        }

        // Fallback: search for pattern alone in the whole text if it's unique enough (like RegNo)
        Pattern lonePattern = Pattern.compile("(" + valuePattern + ")", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher loneMatcher = lonePattern.matcher(fullText);
        if (loneMatcher.find()) {
            String val = loneMatcher.group(1).trim();
            BigDecimal confidence = defaultConfidence.multiply(new BigDecimal("0.7"));
            String status = "PATTERN_MATCH";

            if (!isValid(fieldName, val)) {
                confidence = confidence.multiply(new BigDecimal("0.5"));
                status = "LOW_CONFIDENCE";
            }

            results.put(fieldName, OcrFieldDto.builder()
                    .value(val)
                    .confidence(confidence)
                    .status(status)
                    .build());
        } else {
            results.put(fieldName, OcrFieldDto.builder()
                    .value(null)
                    .confidence(BigDecimal.ZERO)
                    .status("NOT_FOUND")
                    .build());
        }
    }

    private boolean isValid(String fieldName, String value) {
        if (value == null || value.isBlank()) return false;
        return switch (fieldName) {
            case "registrationNumber" -> OcrValidator.isValidPlate(value);
            case "chassisNumber" -> OcrValidator.isValidVin(value);
            case "pinNumber" -> OcrValidator.isValidPin(value);
            case "ownerIdNumber", "ownerId" -> OcrValidator.isValidId(value);
            case "dateOfRegistration", "searchDate" -> OcrValidator.isValidDate(value);
            case "yearOfManufacture" -> OcrValidator.isValidYear(value);
            default -> true;
        };
    }

    protected String getRegNoPattern() {
        // Kenyan RegNo: KXX 000X or KX 000X or KXXX 000X
        return "K[A-Z]{1,3}\\s*\\d{3}[A-Z]";
    }

    protected String getChassisPattern() {
        return "[A-Z0-9]{10,20}";
    }
}
