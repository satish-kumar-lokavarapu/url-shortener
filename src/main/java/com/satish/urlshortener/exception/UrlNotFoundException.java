package com.satish.urlshortener.exception;

/**
 * Thrown when a short code does not exist in the database.
 * Later this becomes a 404 (Not Found) response.
 */
public class UrlNotFoundException extends RuntimeException {

    public UrlNotFoundException(String shortCode) {
        super("Short code not found: " + shortCode);
    }
}