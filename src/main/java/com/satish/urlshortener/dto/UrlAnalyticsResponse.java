package com.satish.urlshortener.dto;

import java.time.Instant;

/**
 * JSON answer of the analytics endpoint.
 * Shows how a short link is used.
 * lastAccessedAt is null when the link was never opened.
 * expired tells if the link still works.
 */
public record UrlAnalyticsResponse(
        String shortCode,
        String originalUrl,
        long clickCount,
        Instant createdAt,
        Instant lastAccessedAt,
        Instant expiresAt,
        boolean expired
) {
}