package com.satish.urlshortener.service;

import com.satish.urlshortener.config.ShortenerProperties;
import com.satish.urlshortener.exception.InvalidUrlException;
import com.satish.urlshortener.exception.ShortCodeGenerationException;
import com.satish.urlshortener.exception.UrlExpiredException;
import com.satish.urlshortener.exception.UrlNotFoundException;
import com.satish.urlshortener.model.UrlMapping;
import com.satish.urlshortener.repository.UrlMappingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;

/**
 * Main business logic of the URL shortener.
 * - shorten: validate the URL, create a unique code, save it
 * - resolve: find the original URL for a short code, count the click
 * - getMapping: read a mapping for the analytics endpoint
 */
@Service
public class UrlService {

    private static final int MAX_URL_LENGTH = 2048;

    private final UrlMappingRepository repository;
    private final ShortCodeGenerator codeGenerator;
    private final ShortenerProperties properties;

    public UrlService(UrlMappingRepository repository,
                      ShortCodeGenerator codeGenerator,
                      ShortenerProperties properties) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.properties = properties;
    }

    /**
     * Creates a short code for the given URL and saves the mapping.
     * expiresAt is optional: NULL means the link never expires.
     *
     * Collision handling: if the generated code already exists,
     * we try again with a new code, up to maxCollisionRetries times.
     * The UNIQUE constraint in the database is the final safety net:
     * even if two requests generate the same code at the same time,
     * the database accepts only one, and the other request retries.
     */
    @Transactional
    public UrlMapping shorten(String originalUrl, Instant expiresAt) {
        String validatedUrl = validateUrl(originalUrl);

        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new InvalidUrlException("expiresAt must be a time in the future");
        }

        int maxTries = properties.maxCollisionRetries();
        for (int attempt = 1; attempt <= maxTries; attempt++) {
            String code = codeGenerator.generate();

            // Fast check first: skip codes we already know are taken.
            if (repository.existsByShortCode(code)) {
                continue;
            }

            try {
                UrlMapping mapping = new UrlMapping(code, validatedUrl, Instant.now(), expiresAt);
                return repository.saveAndFlush(mapping);
            } catch (DataIntegrityViolationException e) {
                // Another request saved the same code a moment before us.
                // Loop again and try with a fresh code.
            }
        }

        throw new ShortCodeGenerationException(
                "Could not generate a unique short code after " + maxTries + " tries");
    }

    /**
     * Returns the mapping for a short code and counts the click.
     * Throws 404 if the code is unknown, 410 if the link has expired.
     * Expired or unknown links are NOT counted.
     */
    @Transactional
    public UrlMapping resolve(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (mapping.isExpired()) {
            throw new UrlExpiredException(shortCode);
        }

        repository.incrementClickCount(shortCode, Instant.now());

        return mapping;
    }

    /**
     * Returns the mapping for the analytics endpoint.
     * Does NOT count a click: looking at statistics is not a visit.
     * Works also for expired links, so old statistics stay readable.
     */
    @Transactional(readOnly = true)
    public UrlMapping getMapping(String shortCode) {
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
    }

    /**
     * Builds the full short URL to return to the user,
     * for example http://localhost:8080/aB3xY9k
     */
    public String buildShortUrl(String shortCode) {
        return properties.baseUrl() + "/" + shortCode;
    }

    /**
     * Checks the URL is safe to accept:
     * - not empty, not too long
     * - a valid URL format
     * - only http or https (blocks things like javascript: or file:)
     * Returns the trimmed URL if all checks pass.
     */
    private String validateUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new InvalidUrlException("URL must not be empty");
        }

        String url = originalUrl.trim();

        if (url.length() > MAX_URL_LENGTH) {
            throw new InvalidUrlException("URL is too long (max " + MAX_URL_LENGTH + " characters)");
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            throw new InvalidUrlException("URL format is not valid");
        }

        String scheme = uri.getScheme();
        if (scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new InvalidUrlException("Only http and https URLs are allowed");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidUrlException("URL must have a host, for example https://example.com");
        }

        return url;
    }
}