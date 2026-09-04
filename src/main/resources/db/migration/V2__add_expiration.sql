-- V2: add optional expiration time for short URLs.
-- NULL means the link never expires, so all old rows keep working.

ALTER TABLE url_mapping
    ADD COLUMN expires_at TIMESTAMP NULL;