package com.isec.platform.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@UtilityClass
public class JwtTokenUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Decodes a JWT token payload without validating the signature.
     * Extracts the 'sub' claim and returns it.
     *
     * @param token The JWT access token
     * @return The 'sub' claim value
     * @throws IllegalArgumentException if the token is invalid or 'sub' is missing/blank
     */
    public static String extractSubject(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT token format");
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode payload = objectMapper.readTree(payloadJson);

            JsonNode subNode = payload.get("sub");
            if (subNode == null || subNode.isNull() || subNode.asText().isBlank()) {
                throw new IllegalArgumentException("JWT token is missing 'sub' claim");
            }

            return subNode.asText();
        } catch (Exception e) {
            log.error("Failed to decode JWT token payload", e);
            throw new IllegalArgumentException("Failed to extract subject from token: " + e.getMessage());
        }
    }
}
