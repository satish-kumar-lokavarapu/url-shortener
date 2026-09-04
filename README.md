# AI-Assisted URL Shortener

## Overview

A URL shortening service that converts long HTTP/HTTPS URLs into compact
short links, redirects users to the original destination, and tracks
basic usage analytics.

Example: `https://spring.io/projects/spring-integration` becomes
`http://localhost:8080/fK489kP`

This project was built as an AI-assisted engineering assignment. AI was
used as an assistant on individual tasks; every piece of generated code
was reviewed, tested, and in several cases corrected or rejected. The
work was done in three scenarios: build the core feature (greenfield),
add expiration to existing code (brownfield), and implement a vague
"add analytics" requirement (ambiguous).

Detailed documentation lives in the `docs/` folder:

| File | Content |
|------|---------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | components, request flow, database design |
| [docs/AI_ENGINEERING_LOG.md](docs/AI_ENGINEERING_LOG.md) | where AI was used, what it got wrong, what I changed |
| [docs/SCENARIOS.md](docs/SCENARIOS.md) | the three scenarios from requirement to validation |
| [docs/TESTING.md](docs/TESTING.md) | testing approach and what is not covered |
| [docs/DECISIONS.md](docs/DECISIONS.md) | trade-offs, assumptions, risks, limitations |
| [docs/ENGINEERING_SUMMARY.md](docs/ENGINEERING_SUMMARY.md) | final engineering summary: plan, artifacts, validation, assumptions, limits |

## Features

- Shorten any http/https URL to a 7-character code
- Redirect short links to the original URL (302)
- Optional expiration time per link; expired links answer 410 Gone
- Click analytics per link: total clicks and last access time
- Input validation and one JSON error shape for all errors (400/404/410/500)
- Versioned database schema with Flyway (V1 core, V2 expiration, V3 analytics)
- Health endpoint and Swagger UI

## Tech Stack

- Java 17, Spring Boot 4 (Web MVC, Data JPA, Validation, Actuator)
- MySQL 8 (Docker or local install), H2 in-memory for tests
- Flyway for schema migrations
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, MockMvc
- Maven (wrapper included), Docker Compose

## Architecture

    Client (browser / curl / Postman)
            |
            v
    UrlController          REST endpoints, HTTP codes
            |
            v
    UrlService             validation, expiry check, click counting
       |            \
       v             v
    ShortCodeGenerator   UrlMappingRepository
    (random Base62)          |
                             v
                          MySQL
                    (schema built by Flyway)

More detail in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## API Endpoints

| Method | Path | What it does |
|--------|------|--------------|
| POST | /api/urls | create a short URL (optional expiresAt) |
| GET | /{shortCode} | redirect to the original URL, counts one click |
| GET | /api/urls/{shortCode}/analytics | usage statistics for a link |
| GET | /actuator/health | health check |

Swagger UI: http://localhost:8080/swagger-ui.html

## Database Schema

One table, `url_mapping`, built step by step by three Flyway scripts:

| Column | Type | Added in | Meaning |
|--------|------|----------|---------|
| id | BIGINT, auto increment | V1 | primary key |
| short_code | VARCHAR(16), UNIQUE | V1 | the short code |
| original_url | VARCHAR(2048) | V1 | the long URL |
| created_at | TIMESTAMP | V1 | when the link was made |
| expires_at | TIMESTAMP, nullable | V2 | when the link dies, NULL = never |
| click_count | BIGINT, default 0 | V3 | how many times it was opened |
| last_accessed_at | TIMESTAMP, nullable | V3 | last open time, NULL = never |

The UNIQUE constraint on `short_code` is the final consistency guard
against collisions. Application-level generation reduces the chance of
a collision, while the database remains the authoritative enforcement
layer.

## Running Locally

You need Java 17. Maven is not needed (the wrapper is included).

    git clone <repo-url>
    cd url-shortener

Get MySQL running, either way works:

**Option 1 - Docker:**

    docker compose up -d

**Option 2 - MySQL installed on your machine** (create the database once):

    mysql -u root -p < scripts/local-mysql-setup.sql

The Docker Compose setup uses development-only database credentials.
Connection settings can be overridden using `DB_USERNAME`,
`DB_PASSWORD`, `DB_HOST`, `DB_PORT`, and `DB_NAME`.

Start the app:

    ./mvnw spring-boot:run

Flyway creates the tables automatically on first start. Check it is up:

    curl http://localhost:8080/actuator/health

## Example Requests

These are real responses from a local run of this project.

### Create a short URL

    curl -X POST http://localhost:8080/api/urls \
      -H "Content-Type: application/json" \
      -d '{"url": "https://spring.io/projects/spring-integration"}'

Response (201 Created):

    {
      "shortCode": "fK489kP",
      "shortUrl": "http://localhost:8080/fK489kP",
      "originalUrl": "https://spring.io/projects/spring-integration",
      "createdAt": "2026-09-04T23:15:52.504452Z",
      "expiresAt": null
    }

With an expiry time (optional - leave it out and the link never expires):

    curl -X POST http://localhost:8080/api/urls \
      -H "Content-Type: application/json" \
      -d '{"url": "https://spring.io/projects/spring-integration", "expiresAt": "2026-12-31T23:59:59Z"}'

### Open a short link

    curl -i http://localhost:8080/fK489kP

Response:

    HTTP/1.1 302
    Location: https://spring.io/projects/spring-integration

Open the same address in a browser and it lands on the Spring
Integration page.

Note: Postman and browsers may automatically follow the redirect and
display the destination response. Use `curl -i` to inspect the
shortener's original `302` response and `Location` header directly.

### Get analytics

After opening the link 5 times:

    curl http://localhost:8080/api/urls/fK489kP/analytics

Response (200):

    {
      "shortCode": "fK489kP",
      "originalUrl": "https://spring.io/projects/spring-integration",
      "clickCount": 5,
      "createdAt": "2026-09-04T23:15:53Z",
      "lastAccessedAt": "2026-09-04T23:16:45Z",
      "expiresAt": null,
      "expired": false
    }

### Errors

All errors use the same JSON shape:

    { "status": 404, "error": "Short code not found: abc9999", "timestamp": "..." }

| Case | Status |
|------|--------|
| empty url, bad url, ftp://, javascript: | 400 |
| expiresAt in the past | 400 |
| body missing or broken JSON | 400 |
| unknown short code | 404 |
| link expired | 410 |
| could not generate a free code | 500 |

## Testing

    ./mvnw test

29 tests: unit tests for the code generator, unit tests for the service
with mocked repository, and integration tests that exercise the full
Spring MVC request pipeline against an H2 database built by the same
Flyway migrations. Manual testing with curl and Postman on top.

Full approach, including what is not covered: [docs/TESTING.md](docs/TESTING.md)

## AI-Assisted Engineering Approach

The working style for every task: I defined the task and constraints,
AI proposed code, I reviewed it, tested it, and fixed or rejected what
was wrong. AI was wrong several times in ways that mattered - for
example the Flyway setup it suggested silently did not run on Spring
Boot 4, and a migration it wrote worked on MySQL but broke on H2. Both
were caught by checking logs and running tests, not by trusting the
output.

AI was used only for bounded engineering tasks with technical context,
constraints, and acceptance criteria. Generated output was never
accepted by default: each change was reviewed, compiled, tested, and
either accepted, edited, or rejected. No credentials, secrets,
production data, or proprietary customer information were provided to
AI tools.

The complete log with tasks, corrections and rejected suggestions:
[docs/AI_ENGINEERING_LOG.md](docs/AI_ENGINEERING_LOG.md)

## Scenarios

1. **Greenfield** - build core URL shortening from nothing
2. **Brownfield** - add expiration to the existing implementation
3. **Ambiguous** - "add analytics": document assumptions, then implement

Each one from requirement to validation: [docs/SCENARIOS.md](docs/SCENARIOS.md)

## Risks and Trade-offs

Short version: collisions are stopped by the UNIQUE constraint plus
retry; dangerous URLs (javascript:, file:) are rejected; parallel
clicks are counted with an atomic database update; internal errors are
logged but not shown to users. Full table with trade-offs and rejected
alternatives: [docs/DECISIONS.md](docs/DECISIONS.md)

## Limitations

- No login and no rate limiting - anyone can create links
- Every click counts - no bot filtering, no unique visitors
- Expired rows stay in the database
- One instance, one database - no cache layer
- Automated integration tests use H2 instead of MySQL, so
  database-engine-specific behavior is additionally verified manually
  against MySQL

## Future Improvements

- Cleanup job that deletes expired links
- Rate limiting on link creation
- Per-day click statistics instead of one total number
- Custom short codes chosen by the user
- Replace H2 integration tests with MySQL Testcontainers for closer
  production parity
- Docker image for the app itself