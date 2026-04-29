package com.isec.platform.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private ServerHttpRequest serverRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        serverRequest = mock(ServerHttpRequest.class);
        org.springframework.http.server.RequestPath requestPath = mock(org.springframework.http.server.RequestPath.class);
        when(requestPath.value()).thenReturn("/api/test");
        when(serverRequest.getPath()).thenReturn(requestPath);
    }

    @Test
    void handleAccessDeniedException_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(ex, serverRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(Objects.requireNonNull(response.getBody()).getStatus()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).contains("Access denied");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    void handleGlobalException_returns500() {
        Exception ex = new RuntimeException("Unexpected");
        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex, serverRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(Objects.requireNonNull(response.getBody()).getStatus()).isEqualTo(500);
    }
}
