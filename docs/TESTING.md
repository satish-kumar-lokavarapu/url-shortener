# Testing

Run everything:

    ./mvnw test

29 automated tests, all green, plus a manual test pass with curl and
Postman.

## Layers

### Unit tests - ShortCodeGeneratorTest (4 tests)

Plain JUnit, no Spring, runs in milliseconds. The generator is built
by hand with test settings, which works because it takes its
configuration through the constructor.

- code has the configured length (7)
- code uses only Base62 characters
- 1000 generated codes are all different
- a different configured length is respected

### Unit tests - UrlServiceTest (15 tests)

Mockito: repository and generator are mocks, so only the service logic
is tested and every branch can be forced.

- valid URL is saved and returned
- rejected: empty URL, ftp://, javascript:, too long, no host
- collision retry: first code taken, second code used
- all retries taken -> ShortCodeGenerationException, nothing saved
- resolve returns mapping, unknown code -> UrlNotFoundException
- expiry: past expiresAt rejected on create, future accepted,
  expired link -> UrlExpiredException on resolve
- analytics rules: resolve counts the click; expired resolve does not
  count; reading analytics does not count
- short URL building joins base URL and code

### Integration tests - UrlControllerIntegrationTest (8 tests)

@SpringBootTest with MockMvc and the "test" profile: the tests exercise
the full Spring MVC request pipeline - controller, service, repository
- against an in-memory H2 database in MySQL mode, migrated by the same
  Flyway scripts as production. Real data is never touched. The table is
  emptied before each test so tests do not affect each other.

- POST creates a link: 201, 7-character code, all fields present
- created link redirects: 302 with the right Location header
- unknown code: 404 with the JSON error shape
- invalid URL: 400; empty URL: 400; missing body: 400 (regression test
  for a bug found manually)
- three redirects -> analytics shows clickCount 3 and a last access time
- analytics for unknown code: 404

## Manual testing

Every endpoint and error case was also exercised by hand with curl and
Postman, including the full expiry lifecycle in real time (302 while
alive, 410 after expiry, statistics still readable) and click counting
against both the local MySQL and the Docker MySQL.

Manual testing earned its place: it found the missing-request-body case
answering 500 instead of 400, which no automated test covered at the
time. Fixed, and locked in with a regression test.

## Why H2 for tests

H2 in MySQL mode starts instantly, needs no infrastructure, and runs
the same Flyway scripts so the schema matches production. The known
cost: H2 is close to MySQL but not identical. That cost became real
once - a migration valid on MySQL failed on H2 (two ADD COLUMN in one
ALTER TABLE) - and the test suite is exactly what caught it. The
production-grade upgrade would be Testcontainers (tests against a real
MySQL in Docker); for this prototype H2's speed and simplicity won,
and MySQL-specific behavior is additionally verified manually against
a real MySQL instance.

## Quality gates used during development

- every new file: compile before moving on
- every feature: tests green before commit
- every feature: manual curl/Postman check on top
- app start with ddl-auto=validate acts as a schema gate: if entity and
  migrations drift, the app refuses to boot

## Security review

Security validation for this prototype included:

- only HTTP/HTTPS URLs are accepted
- unsupported schemes such as javascript: and file: are rejected
- URL length is bounded
- internal exception details are not returned to clients
- database credentials can be overridden through environment variables
- no secrets, production data, or proprietary customer information were
  supplied to AI tools

Not covered:
- SAST/dependency vulnerability scanning
- penetration testing
- authentication/authorization
- rate limiting

## Performance review

No formal load test was performed.

Performance-sensitive design choices include:

- indexed/unique short-code lookup
- atomic SQL increment for click counting
- no Java read-modify-write cycle for analytics updates
- no unnecessary distributed infrastructure for the prototype scope

Not covered:
- throughput benchmark
- p95/p99 latency measurement
- sustained concurrency testing

## Not covered (summary)

- load and performance testing
- true parallel-click stress testing (the atomic database update is
  intended to prevent lost increments under concurrent redirects, but
  this behavior was not stress-tested with parallel traffic)
- security scanning / penetration testing
- repository-layer tests against real MySQL (Testcontainers)