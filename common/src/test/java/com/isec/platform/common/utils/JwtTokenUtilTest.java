package com.isec.platform.common.utils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenUtilTest {

    @Test
    void shouldExtractSubjectFromValidToken() {
        String payload = "{\"sub\":\"12345\",\"name\":\"John Doe\"}";
        String encodedPayload = Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String token = "header." + encodedPayload + ".signature";

        String sub = JwtTokenUtil.extractSubject(token);
        assertEquals("12345", sub);
    }

    @Test
    void shouldThrowExceptionForMissingSub() {
        String payload = "{\"name\":\"John Doe\"}";
        String encodedPayload = Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String token = "header." + encodedPayload + ".signature";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> JwtTokenUtil.extractSubject(token));
        assertTrue(exception.getMessage().contains("missing 'sub' claim"));
    }

    @Test
    void shouldThrowExceptionForBlankSub() {
        String payload = "{\"sub\":\"  \"}";
        String encodedPayload = Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String token = "header." + encodedPayload + ".signature";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> JwtTokenUtil.extractSubject(token));
        assertTrue(exception.getMessage().contains("missing 'sub' claim"));
    }

    @Test
    void shouldThrowExceptionForInvalidFormat() {
        String token = "invalidtoken";
        assertThrows(IllegalArgumentException.class, () -> JwtTokenUtil.extractSubject(token));
    }

    @Test
    void shouldThrowExceptionForNullOrEmptyToken() {
        assertThrows(IllegalArgumentException.class, () -> JwtTokenUtil.extractSubject(null));
        assertThrows(IllegalArgumentException.class, () -> JwtTokenUtil.extractSubject(""));
    }
}
