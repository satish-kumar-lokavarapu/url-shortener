-- V1: first table for the URL shortener.
-- Stores the short code and the original long URL.
-- short_code has a UNIQUE constraint so two rows can never
-- have the same code. This keeps every short link safe and unique.

CREATE TABLE url_mapping (
                             id            BIGINT        NOT NULL AUTO_INCREMENT,
                             short_code    VARCHAR(16)   NOT NULL,
                             original_url  VARCHAR(2048) NOT NULL,
                             created_at    TIMESTAMP     NOT NULL,
                             PRIMARY KEY (id),
                             CONSTRAINT uk_url_mapping_short_code UNIQUE (short_code)
);