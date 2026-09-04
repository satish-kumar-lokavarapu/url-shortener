package com.satish.urlshortener.exception;

/**
 * Thrown when we could not create a free short code
 * after all retries. Very rare. Becomes a 500 response.
 */
public class ShortCodeGenerationException extends RuntimeException {

    public ShortCodeGenerationException(String message) {
        super(message);
    }
}