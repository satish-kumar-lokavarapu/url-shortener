package com.satish.urlshortener.dto;

import java.time.Instant;

/**
 * JSON answer after creating a short URL.
 * expiresAt is null when the link never expires.
 */
public record ShortenUrlResponse(
        String shortCode,
        String shortUrl,
        String originalUrl,
        Instant createdAt,
        Instant expiresAt
) {
}