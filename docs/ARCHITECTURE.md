# Architecture

## Components and request flow

    Client (browser / curl / Postman)
            |
            v
    UrlController
      - POST /api/urls
      - GET /{shortCode}
      - GET /api/urls/{shortCode}/analytics
      - turns service results into HTTP answers (201, 302, JSON)
            |
            v
    UrlService
      - validates the URL (http/https only, length, real host)
      - checks expiresAt is in the future
      - picks a free short code (retry on collision)
      - checks expiry on redirect and counts the click
            |
       -----+-----
       |         |
       v         v
    ShortCodeGenerator     UrlMappingRepository
    (random 7-char           (find by code, exists check,
     Base62 codes,            save, atomic click update)
     SecureRandom)                  |
                                    v
                                 MySQL
                        (tables built by Flyway)

GlobalExceptionHandler sits over all controllers and turns every
exception into one JSON error shape with the right status code.

## Flow of one create request

1. JSON body arrives at UrlController, bean validation runs first
   (@NotBlank, @Size)
2. UrlService validates the URL deeper (scheme, host) and the expiry time
3. ShortCodeGenerator makes a random code; service checks it is free
4. Row is saved; if another request took the same code in that moment,
   the database UNIQUE constraint rejects it and the service retries
   with a fresh code (max 3 tries)
5. Controller answers 201 with the mapping as JSON

## Flow of one redirect

1. GET /{shortCode} arrives
2. Service loads the mapping (404 if unknown)
3. Expiry check (410 if past expiresAt)
4. One SQL statement adds 1 to click_count and stamps last_accessed_at
5. Controller answers 302 with Location = original URL

## Database design

One table, url_mapping. It grew through three Flyway migrations, on
purpose, to match the three scenarios of the assignment:

- V1__init.sql - id, short_code (UNIQUE), original_url, created_at
- V2__add_expiration.sql - expires_at (nullable, NULL = never expires)
- V3__add_analytics.sql - click_count (default 0), last_accessed_at

Hibernate is set to ddl-auto=validate: it never creates or changes
tables, it only checks the entity matches what Flyway built, and the
app refuses to start if they drift.

Notes:
- short_code is VARCHAR(16) while codes are 7 characters, so the code
  length can grow through configuration without a schema change
- original_url is VARCHAR(2048), a practical URL length ceiling,
  also enforced at the API layer so users get a clean 400
- the UNIQUE constraint has an explicit name (uk_url_mapping_short_code)
  so future migrations and error messages are predictable

## Package structure

    com.satish.urlshortener
      controller/   UrlController
      service/      UrlService, ShortCodeGenerator
      repository/   UrlMappingRepository
      model/        UrlMapping (JPA entity)
      dto/          ShortenUrlRequest, ShortenUrlResponse,
                    UrlAnalyticsResponse, ErrorResponse
      exception/    InvalidUrlException, UrlNotFoundException,
                    UrlExpiredException, ShortCodeGenerationException,
                    GlobalExceptionHandler
      config/       ShortenerProperties (typed settings)

Each layer only talks to the next one: controller -> service ->
repository. DTOs keep the API shape separate from the database entity.

## Configuration

All app settings live in application.properties. The three business
settings are grouped under app.shortener and bound to one record class
(ShortenerProperties):

    app.shortener.base-url=http://localhost:8080
    app.shortener.code-length=7
    app.shortener.max-collision-retries=3

Database credentials use the ${VAR:default} form: development-only
defaults are in the file, and any environment can override them with
environment variables without touching code.

Tests use a separate profile (application-test.properties) that points
to an in-memory H2 database in MySQL mode, migrated by the same Flyway
scripts.