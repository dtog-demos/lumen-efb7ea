package com.example.weather.ingestion;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import com.example.weather.common.ApiError;
import com.example.weather.common.ConflictException;
import com.example.weather.common.GlobalExceptionHandler;
import com.example.weather.common.InvalidRequestException;

class GlobalExceptionHandlerTest {
    // InvalidRequestException maps to 400 with its own message
    @Test
    void mapsInvalidRequestTo400() {
        ResponseEntity<ApiError> response = new GlobalExceptionHandler()
                .invalid(new InvalidRequestException("bad input"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad input", response.getBody().message());
        assertTrue(response.getBody().details().isEmpty());
    }

    @Test
    void mapsConflictingRequestTo409() {
        ResponseEntity<ApiError> response = new GlobalExceptionHandler()
                .conflict(new ConflictException(
                        "Reading values conflict with existing readings",
                        List.of("temperature")));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Reading values conflict with existing readings", response.getBody().message());
        assertEquals(List.of("temperature"), response.getBody().details());
    }

    // unexpected errors map to 500 and do not leak details
    @Test
    void hidesDetailsOfUnexpectedErrors() {
        ResponseEntity<ApiError> response = new GlobalExceptionHandler()
                .unexpected(new RuntimeException("secret details"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().message().contains("secret"));
        assertTrue(response.getBody().details().isEmpty());
    }

    // Spring's own client errors keep their status (here 405) instead of becoming a 500
    @Test
    void keepsStatusOfFrameworkClientErrors() {
        ResponseEntity<ApiError> response = new GlobalExceptionHandler()
                .unexpected(new HttpRequestMethodNotSupportedException("GET"));

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(405, response.getBody().status());
    }
}
