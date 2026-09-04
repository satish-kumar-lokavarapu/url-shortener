# Decisions, Trade-offs, Risks and Limitations

## Major decisions and why

### Random codes, not URL hashes

Codes are 7 random Base62 characters from SecureRandom (about 3.5
trillion combinations). A hash of the URL was considered and rejected:
the same URL would always give the same code (leaks whether someone
already shortened a URL, and blocks having several links for one URL),
and truncated hashes collide more than people expect. Random + a
uniqueness guard is simpler and safer.

### Same URL twice gives two codes

No lookup for "was this URL shortened before". Reasons: simpler code,
no need to index a 2048-character column, and separate links for the
same URL are genuinely useful (one link per campaign, each with its own
click count). Trade-off: more rows if many people shorten the same
popular URL - acceptable at prototype scale.

### 302 redirect, not 301

Permanent redirects may be cached aggressively by clients, which can
bypass the service on later requests and make click analytics less
reliable. 302 keeps clicks flowing through the service. Trade-off:
slightly more traffic to our server - that is the point.

### Collision handling in two layers

Fast existsByShortCode check first, database UNIQUE constraint as the
final guard. If two requests generate the same code in the same
moment, the database rejects one insert and that request retries with
a fresh code. If all configured retries are exhausted, the request
fails with an explicit server-side error rather than looping
indefinitely. The service uses saveAndFlush so the constraint
violation happens inside the try/catch, not later at transaction
commit.

### Click counting as one atomic SQL statement

UPDATE ... SET click_count = click_count + 1 is executed by the
database. Because the increment is performed atomically by the
database, concurrent updates avoid the lost-update problem that can
occur with a Java read-modify-write cycle. Chosen over a message queue
or cache-based counter as the simplest correct solution at this scale.

### Flyway owns the schema, Hibernate only validates

Versioned SQL scripts (V1, V2, V3) are the only way tables change;
ddl-auto=validate makes the app refuse to start if entities and schema
drift. Chosen over ddl-auto=update because migrations are reviewable,
ordered, and made the brownfield scenario a real visible schema
evolution. Trade-off: slightly more setup and one hard lesson (see
Risks below).

### Breaking the entity constructor (3 -> 4 parameters)

When expiration was added, the constructor changed instead of adding a
compatible overload. For a shared library that would be wrong; here all
three callers are inside this project and were updated in the same
commit. The compile errors served as a complete checklist of code that
needed to consider expiration. A convenience constructor would let
future code silently ignore the feature.

### Optional expiresAt, NULL = never expires

The common case (just shorten a link) stays one-field simple, and all
links created before the feature keep working unchanged.

### 410 Gone for expired links

410 means "existed, gone now"; 404 means "never existed". The
distinction is real information for API users.

### H2 for tests, MySQL for runtime

Fast, zero-infrastructure tests running the same Flyway scripts. Known
risk (H2 is not exactly MySQL) accepted and it fired once - caught by
the tests themselves. Testcontainers noted as the upgrade path;
MySQL-specific behavior additionally verified manually.

### Docker Compose as an optional MySQL path

The repo works with a local MySQL install or `docker compose up -d`,
same configuration either way. Compose exists mainly so anyone can run
the project with zero database setup. The app itself is not
containerized - not required for the assignment, mvnw covers
"runnable".

## Risk table

| Risk | Mitigation |
|------|------------|
| Two links get the same code | UNIQUE constraint + retry (max 3) + clear failure |
| Invalid or garbage URL | bean validation + URI parsing + host check -> 400 |
| Malicious URL (javascript:, file:) | only http/https schemes accepted |
| Very long URL | 2048 limit at API and database layer |
| Unknown short code | 404 with clean JSON |
| Expired link | 410 Gone, click not counted |
| Lost clicks under parallel traffic | atomic database update |
| Internal details leaking in errors | catch-all handler logs details, user sees a generic message |
| Schema drift between code and database | ddl-auto=validate refuses to start |
| Broken request body | dedicated 400 handler (bug found by manual testing) |
| Credentials in source control | development-only defaults, real values via environment variables |
| Link-creation abuse | No authentication/rate limiting in the prototype; production would add identity, quotas, and rate controls |
| Phishing / malicious destination abuse | HTTP/HTTPS scheme validation prevents unsafe schemes, but production would also need abuse monitoring and possibly domain reputation/blocking controls |

## Assumptions

- Every successful redirect is one click; bots and repeat visitors count
- Reading analytics is not a visit
- Analytics of expired links stay readable
- Local development may use disposable development credentials.
  Production would use a dedicated least-privilege database account with
  credentials supplied through environment-specific secret management.
- Single instance, moderate traffic - no cache or queue needed yet

## Limitations

- No authentication and no rate limiting
- No unique-visitor or per-day statistics, one total counter
- Expired rows are never deleted
- No custom (user-chosen) short codes
- H2-based tests do not catch every MySQL-specific behavior;
  MySQL-specific behavior is additionally verified manually

## Rejected alternatives

- Hash-based short codes (see above)
- Redis/Kafka-style infrastructure from a reference implementation:
  over-scoped for a 2-3 day prototype; the operational complexity is
  not justified by the requirements. Could be introduced later as a
  cache for high-volume redirects or an event stream for analytics.
- Keeping a backward-compatible entity constructor (see above)
- ddl-auto=update instead of Flyway: no migration history, silent
  drift, weaker change management
- Testcontainers from the start: closer MySQL parity, but adds Docker
  dependency and slower test startup. H2 was chosen for prototype speed,
  with manual MySQL validation and Testcontainers documented as the
  production-grade upgrade path.

## Future improvements

- Scheduled cleanup of expired links
- Rate limiting on creation
- Per-day click statistics
- Custom short codes with a reserved-word list
- Testcontainers for MySQL-accurate integration tests
- Dockerfile for the application itself