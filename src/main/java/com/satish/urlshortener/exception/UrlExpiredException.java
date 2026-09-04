package com.satish.urlshortener.exception;

/**
 * Thrown when a short link exists but its expiry time has passed.
 * Later this becomes a 410 (Gone) response.
 * 410 means: "this existed before, but it is gone now" -
 * more exact than 404, which means "never heard of it".
 */
public class UrlExpiredException extends RuntimeException {

    public UrlExpiredException(String shortCode) {
        super("Short link has expired: " + shortCode);
    }
}