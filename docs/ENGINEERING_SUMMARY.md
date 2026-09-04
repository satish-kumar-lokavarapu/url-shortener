# Final Engineering Summary

This is the closing summary the assignment asks for: the plan, what was
produced, how it was validated, and what its limits are. Details live in
the linked documents; this page is the map.

## 1. Problem

Build a URL shortener as a production-style prototype in 2-3 days,
using AI as an assistant while the engineer owns every decision. Three
required situations: build new (greenfield), change existing code
(brownfield), and implement an unclear requirement (ambiguous).

## 2. Plan and rationale

The work was planned as phases, each ending in a git commit, so the
history itself shows the three scenarios:

    Phase 0  skeleton: Spring Boot, MySQL, Flyway, health, Swagger
    Phase 1  greenfield: create + redirect, validation, errors, tests
    Phase 2  brownfield: expiration (V2 migration, 410 Gone)
    Phase 3  ambiguous: analytics (assumptions first, V3, atomic count)
    Phase 4  documentation and packaging (Docker Compose, docs/)

Rationale for the biggest choices (full list in
[DECISIONS.md](DECISIONS.md)): random codes instead of URL hashes,
302 instead of 301 (permanent redirects may be cached aggressively and
bypass the service), collision safety via UNIQUE constraint plus retry,
click counting as one atomic SQL statement, Flyway as the only owner of
the schema, and deliberately NOT copying a Kafka/Redis-scale reference
design - too much infrastructure for the requirement.

## 3. Artifacts produced

- Working service: 3 core REST endpoints, plus health and Swagger/OpenAPI,
  runnable with `docker compose up -d` and `./mvnw spring-boot:run`
- Database schema in 3 versioned Flyway migrations (V1 core,
  V2 expiration, V3 analytics)
- 29 automated tests (unit, mocked-service, and integration tests
  exercising the full Spring MVC pipeline against H2)
- Documentation set: [README](../README.md),
  [ARCHITECTURE.md](ARCHITECTURE.md), [SCENARIOS.md](SCENARIOS.md),
  [AI_ENGINEERING_LOG.md](AI_ENGINEERING_LOG.md),
  [TESTING.md](TESTING.md), [DECISIONS.md](DECISIONS.md)
- Git history with one commit per meaningful step

## 4. AI-assisted workflow (and where AI was wrong)

Tasks were given to AI one at a time with context, constraints and
acceptance criteria; every output was hand-added, compiled, tested and
reviewed before the next step. Traceability of accepted / edited /
rejected is kept in [AI_ENGINEERING_LOG.md](AI_ENGINEERING_LOG.md).
The log includes real corrections: Flyway silently not running on
Spring Boot 4 (wrong dependency suggested), a test annotation from a
moved package, a migration that passed MySQL but failed H2, a 500
answer for a missing request body found by manual testing, and rejected
suggestions with rationale. No credentials, secrets, production data,
or proprietary information were provided to AI tools. The engineer
approved every change; nothing went in unreviewed.

## 5. Validation

- 29 automated tests, green (see [TESTING.md](TESTING.md)), including a
  security review and performance-aware design review documented there
- Manual curl/Postman pass over every endpoint and error case,
  including the full expiry lifecycle in real time
- Fresh-database proof: starting against an empty Docker MySQL builds
  the whole schema (V1-V3) automatically and the API works end to end
- Startup schema gate: ddl-auto=validate refuses to boot on drift

## 6. Risks and trade-offs (short form)

Collisions -> UNIQUE constraint + bounded retry. Malicious URLs ->
http/https only. Lost parallel clicks -> atomic database increment.
Error detail leaks -> generic 500 message, details only in logs.
Abuse (spam creation, phishing destinations) -> acknowledged as a
production concern requiring identity, quotas and abuse monitoring.
H2-vs-MySQL differences -> accepted for test speed; one compatibility
difference was exposed by the test suite and corrected before submission.
Full table: [DECISIONS.md](DECISIONS.md).

## 7. Assumptions

Every successful redirect counts as a click; bots and repeats count;
reading analytics is not a visit; expired links keep readable
statistics; development credentials are disposable and dev-only, with
production using least-privilege accounts and secret management; single
instance and moderate traffic.

## 8. Limitations and next steps

No auth or rate limiting, one total click counter (no per-day or
unique-visitor stats), expired rows are never cleaned up, no cache
layer, tests do not cover load or sustained-concurrency stress. Next
steps in priority order: authentication/rate limiting, cleanup job,
per-day analytics, Testcontainers, and an application Dockerfile.

## 9. Closing statement

AI accelerated implementation, debugging, and review preparation. The
task breakdown, architecture, accept/edit/reject decisions, corrective
changes, validation, and final production-readiness judgment remained
under engineer ownership.