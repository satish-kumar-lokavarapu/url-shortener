package com.satish.urlshortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reads app.shortener settings from application.properties.
 */
@ConfigurationProperties(prefix = "app.shortener")
public record ShortenerProperties(
        String baseUrl,
        int codeLength,
        int maxCollisionRetries
) {
}