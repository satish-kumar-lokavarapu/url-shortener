package com.satish.urlshortener.dto;

import java.time.Instant;

/**
 * JSON answer after creating a short URL.
 * Example:
 * {
 *   "shortCode": "aB3xY9k",
 *   "shortUrl": "http://localhost:8080/aB3xY9k",
 *   "originalUrl": "https://example.com/some/long/page",
 *   "createdAt": "2026-09-04T18:00:00Z"
 * }
 */
public record ShortenUrlResponse(
        String shortCode,
        String shortUrl,
        String originalUrl,
        Instant createdAt
) {
}