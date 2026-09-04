package com.satish.urlshortener.dto;

import java.time.Instant;

/**
 * JSON shape for all error answers of this API.
 * Example: { "status": 404, "error": "Short code not found: abc1234", "timestamp": "..." }
 * One same shape everywhere makes errors easy to read for API users.
 */
public record ErrorResponse(
        int status,
        String error,
        Instant timestamp
) {

    public static ErrorResponse of(int status, String error) {
        return new ErrorResponse(status, error, Instant.now());
    }
}