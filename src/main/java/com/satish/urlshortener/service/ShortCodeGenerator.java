package com.satish.urlshortener.service;

import com.satish.urlshortener.config.ShortenerProperties;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Creates random short codes like "aB3xY9k".
 *
 * Uses Base62 characters: 0-9, A-Z, a-z (62 characters).
 * With length 7 that gives 62^7 = about 3.5 trillion possible codes,
 * so the chance of creating the same code twice is very small.
 */
@Component
public class ShortCodeGenerator {

    /** The 62 characters allowed in a short code. */
    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * SecureRandom = strong random generator.
     * Normal Random is predictable, so someone could guess
     * the next codes. SecureRandom prevents that.
     */
    private final SecureRandom random = new SecureRandom();

    private final ShortenerProperties properties;

    public ShortCodeGenerator(ShortenerProperties properties) {
        this.properties = properties;
    }

    /**
     * Builds one random code.
     * Picks one random character from ALPHABET, repeats codeLength times.
     */
    public String generate() {
        int length = properties.codeLength();
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(ALPHABET.length());
            code.append(ALPHABET.charAt(index));
        }
        return code.toString();
    }
}