package com.satish.urlshortener.service;

import com.satish.urlshortener.config.ShortenerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ShortCodeGenerator.
 * Plain JUnit tests, no Spring needed: we create the class by hand
 * with test settings, so the tests run fast.
 */
class ShortCodeGeneratorTest {

    private ShortCodeGenerator generator;

    @BeforeEach
    void setUp() {
        // Same values as application.properties: length 7, 3 retries
        ShortenerProperties properties =
                new ShortenerProperties("http://localhost:8080", 7, 3);
        generator = new ShortCodeGenerator(properties);
    }

    @Test
    void generatedCodeHasConfiguredLength() {
        String code = generator.generate();

        assertThat(code).hasSize(7);
    }

    @Test
    void generatedCodeUsesOnlyAllowedCharacters() {
        String code = generator.generate();

        // Base62: only digits and letters, nothing else
        assertThat(code).matches("[0-9A-Za-z]+");
    }

    @Test
    void generatesDifferentCodes() {
        // Generate many codes; they should (almost) all be different.
        // 1000 codes out of 3.5 trillion possibilities:
        // a duplicate here would mean the randomness is broken.
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generate());
        }

        assertThat(codes).hasSize(1000);
    }

    @Test
    void respectsDifferentConfiguredLength() {
        // Check the generator really reads the length from settings
        ShortenerProperties customProperties =
                new ShortenerProperties("http://localhost:8080", 10, 3);
        ShortCodeGenerator customGenerator = new ShortCodeGenerator(customProperties);

        String code = customGenerator.generate();

        assertThat(code).hasSize(10);
    }
}