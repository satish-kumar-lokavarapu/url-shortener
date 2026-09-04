-- V3: analytics columns.
-- click_count: how many times the short link was opened.
-- last_accessed_at: when it was opened last time (NULL = never opened).
-- Existing rows start with 0 clicks.
-- Written as two statements: H2 (used in tests) does not accept
-- two ADD COLUMN in one ALTER TABLE, MySQL does.

ALTER TABLE url_mapping
    ADD COLUMN click_count BIGINT NOT NULL DEFAULT 0;

ALTER TABLE url_mapping
    ADD COLUMN last_accessed_at TIMESTAMP NULL;