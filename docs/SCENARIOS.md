# The Three Scenarios

The assignment asks for three different engineering situations. This
project was built in exactly that order, and the git history shows it:
first the core feature, then a change to existing code, then an unclear
requirement made concrete.

---

## Scenario 1 - Greenfield: build core URL shortening

### Requirement

"Build a URL shortener."

### Understanding

Turned the one-line requirement into concrete rules. The system shall:

1. accept a valid http/https URL
2. generate a unique short code
3. store the code -> URL mapping
4. redirect requests with a known code to the original URL
5. reject invalid input with clear errors
6. answer unknown codes with 404

### Ambiguities settled up front

- Code format? -> random 7 characters, Base62 (0-9 A-Z a-z)
- Same URL twice? -> new code each time (documented decision)
- Which redirect status? -> 302 rather than 301. Permanent redirects
  may be cached aggressively by clients, which can bypass the service
  on later requests and make click analytics less reliable.

### Task decomposition

    database schema (V1)
      -> entity
      -> repository
      -> short code generator
      -> service (validation, collision retry)
      -> controller + DTOs
      -> global error handling
      -> unit tests -> integration tests

Each task was implemented incrementally, with compilation and validation
before moving to the next dependent step.

### AI assistance and engineer decisions

AI generated each file from a described task with constraints. Two
things went wrong and were caught: Flyway silently not running on
Spring Boot 4 (fixed with the correct starter) and a missing request
body returning 500 (found by manual Postman testing, fixed to 400 with
a regression test). Details in AI_ENGINEERING_LOG.md.

### Validation

- 21 automated tests at the end of the scenario (generator, service
  with mocks, and integration tests exercising the full Spring MVC
  pipeline against H2)
- manual curl/Postman pass over every endpoint and error case

### Result

Working create + redirect with validation, collision safety and clean
errors. Commits: project skeleton, core feature, tests, the 400 fix.

---

## Scenario 2 - Brownfield: add URL expiration

### Requirement

"Add expiration support to existing shortened URLs."

### Understanding the existing code first

Walked the existing flow to find where expiration belongs:

    UrlController.redirect -> UrlService.resolve -> repository -> DB

The redirect flow loads the mapping and immediately redirects, so the
expiry check belongs in the service layer, before returning the
destination. The controller only maps the new exception to a status.

### Impacted components (identified before coding)

- database: new nullable expires_at column -> new migration V2
- UrlMapping entity: new field + isExpired() helper
- UrlService: accept optional expiresAt on create (must be in the
  future), check expiry on resolve
- new UrlExpiredException -> 410 Gone in the global handler
- DTOs and controller: optional expiresAt in, expiresAt out
- tests: past-expiry rejected, future accepted, expired resolve -> 410

### Task decomposition

1. inspect the existing redirect and create flows
2. add a nullable expires_at column with a new Flyway migration
3. update the entity and DTOs
4. validate expiresAt during creation
5. enforce expiration during redirect
6. add UrlExpiredException and map it to 410
7. update unit and integration tests
8. manually verify both old and expiring links

### Key decisions

- expires_at is nullable and optional: NULL means never expires, so
  every existing link keeps working untouched - safe schema evolution
- 410 Gone for expired (existed before, gone now) vs 404 (never
  existed) - more precise for API users
- constructor changed from 3 to 4 parameters instead of adding an
  overload; acceptable because all callers are internal (full
  reasoning in DECISIONS.md)

### Validation

- 3 new unit tests + updated existing ones (24 total, all green)
- live test: created a link with a 5-minute expiry, got 302 while
  alive, 410 after the time passed; past expiresAt on create gives 400

### Result

Expiration shipped as a targeted change: one migration, edits in four
existing files, one new exception. No rewrite, old data unaffected.

---

## Scenario 3 - Ambiguous requirement: "add analytics"

### Requirement

"Add analytics for shortened URLs." - that is all.

### Questions the requirement does not answer

- what counts as a click? unique users or every request?
- do bots count? do failed redirects count?
- real-time or delayed? how long is data kept?

### Documented assumptions (the actual engineering deliverable here)

For this prototype:

- every successful redirect counts as one click
- opening an expired (410) or unknown (404) link does not count
- reading the analytics endpoint does not count - checking statistics
  is not a visit
- analytics of an expired link stays readable, history survives the link
- no bot filtering, no unique-visitor tracking, no retention limit -
  out of scope, listed as known limits

### Task decomposition

1. identify unanswered product questions
2. define and document prototype assumptions
3. evolve the schema with analytics fields
4. implement atomic click counting
5. expose the analytics endpoint
6. add service and integration tests
7. validate expired, unknown, and repeated-access behavior

### Execution

- migration V3: click_count (default 0 - old rows start clean) and
  last_accessed_at
- the counter is increased by the database itself in one statement
  (UPDATE ... SET click_count = click_count + 1). Reading the number
  into Java, adding 1 and saving back would lose clicks when two
  arrive at the same moment - both would read the same value. The
  atomic update makes every click land.
- new endpoint GET /api/urls/{shortCode}/analytics returning code, URL,
  clicks, created/last-access/expiry times and an expired flag

### What went wrong and was caught

The V3 migration was written as one ALTER TABLE with two columns. MySQL
accepted it; the H2 test database did not, and the integration tests
failed. Split into two statements; the local development reset needed
to re-apply it also deleted the collected click counts - a first-hand
lesson in why applied migrations are never rolled back in production
(details in AI_ENGINEERING_LOG.md).

### Validation

- 5 new tests: click counted on resolve, not counted for expired, not
  counted when reading analytics, integration count-3-clicks check,
  404 for unknown code (29 total, all green)
- live test: clicked a link, watched clickCount and lastAccessedAt
  move; verified an expired link keeps its statistics readable with
  expired=true

### Result

Analytics shipped with the assumptions written down first, an
implementation that is safe under parallel clicks, and honest known
limits.