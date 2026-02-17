package com.isec.platform.modules.ocr.infrastructure.text;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TextNormalizer {
    public String normalize(String input) {
        if (input == null) return "";
        String s = Normalizer.normalize(input, Normalizer.Form.NFKC)
                .replaceAll("[\u200B-\u200D\uFEFF]", "") // zero-width
                .replaceAll("[\u00A0]", " ") // non-breaking space
                .replaceAll("[\r\t]", " ")
                .replaceAll("[\u2013\u2014]", "-")
                .replaceAll("[^\\p{L}\\p{N}\\-/:,.()&%#\\s]", " ")
                .toUpperCase();
        // collapse whitespace
        s = s.replaceAll(" +", " ").trim();
        return s;
    }

    public List<String> toLines(String normalized) {
        if (normalized == null) return List.of();
        return Arrays.stream(normalized.split("\n"))
                .map(l -> l.replaceAll(" +", " ").trim())
                .filter(l -> !l.isBlank())
                .collect(Collectors.toList());
    }
}
