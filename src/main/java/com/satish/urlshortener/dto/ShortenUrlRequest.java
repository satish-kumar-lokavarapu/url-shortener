package com.satish.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body the user sends to create a short URL.
 * Example: { "url": "https://example.com/some/long/page" }
 */
public record ShortenUrlRequest(

        @NotBlank(message = "url must not be empty")
        @Size(max = 2048, message = "url is too long (max 2048 characters)")
        String url
) {
}