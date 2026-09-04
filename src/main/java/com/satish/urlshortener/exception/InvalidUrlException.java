package com.satish.urlshortener.exception;

/**
 * Thrown when the user sends a URL we cannot accept,
 * for example wrong format or not http/https.
 * Later this becomes a 400 (Bad Request) response.
 */
public class InvalidUrlException extends RuntimeException {

    public InvalidUrlException(String message) {
        super(message);
    }
}