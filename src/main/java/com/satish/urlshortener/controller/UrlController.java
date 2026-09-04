package com.satish.urlshortener.controller;

import com.satish.urlshortener.dto.ShortenUrlRequest;
import com.satish.urlshortener.dto.ShortenUrlResponse;
import com.satish.urlshortener.dto.UrlAnalyticsResponse;
import com.satish.urlshortener.model.UrlMapping;
import com.satish.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST endpoints of the URL shortener.
 * - POST /api/urls                      : create a short URL (optional expiresAt)
 * - GET  /{shortCode}                   : redirect to the original URL
 * - GET  /api/urls/{shortCode}/analytics : usage statistics of a short link
 */
@RestController
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * Creates a short URL.
     * @Valid runs the checks written in ShortenUrlRequest first.
     * Returns 201 (Created) with the mapping details.
     */
    @PostMapping("/api/urls")
    public ResponseEntity<ShortenUrlResponse> shorten(@Valid @RequestBody ShortenUrlRequest request) {
        UrlMapping mapping = urlService.shorten(request.url(), request.expiresAt());

        ShortenUrlResponse response = new ShortenUrlResponse(
                mapping.getShortCode(),
                urlService.buildShortUrl(mapping.getShortCode()),
                mapping.getOriginalUrl(),
                mapping.getCreatedAt(),
                mapping.getExpiresAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Opens a short link: finds the original URL and redirects to it.
     * Every successful redirect counts as one click.
     * Uses 302 (temporary redirect), not 301 (permanent).
     * Reason: browsers remember a 301 forever and stop calling us,
     * then we could never count clicks.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        UrlMapping mapping = urlService.resolve(shortCode);

        return ResponseEntity.status(HttpStatus.FOUND)      // 302
                .location(URI.create(mapping.getOriginalUrl()))
                .build();
    }

    /**
     * Shows usage statistics for a short link.
     * Does not count as a click. Also works for expired links.
     */
    @GetMapping("/api/urls/{shortCode}/analytics")
    public UrlAnalyticsResponse analytics(@PathVariable String shortCode) {
        UrlMapping mapping = urlService.getMapping(shortCode);

        return new UrlAnalyticsResponse(
                mapping.getShortCode(),
                mapping.getOriginalUrl(),
                mapping.getClickCount(),
                mapping.getCreatedAt(),
                mapping.getLastAccessedAt(),
                mapping.getExpiresAt(),
                mapping.isExpired()
        );
    }
}