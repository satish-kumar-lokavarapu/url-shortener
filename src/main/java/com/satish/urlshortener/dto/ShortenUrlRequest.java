package com.satish.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * JSON body the user sends to create a short URL.
 * expiresAt is optional: leave it out and the link never expires.
 *
 * Example:
 * { "url": "https://example.com/page" }
 * or
 * { "url": "https://example.com/page", "expiresAt": "2026-12-31T23:59:59Z" }
 */
public record ShortenUrlRequest(

        @NotBlank(message = "url must not be empty")
        @Size(max = 2048, message = "url is too long (max 2048 characters)")
        String url,

        Instant expiresAt
) {
}