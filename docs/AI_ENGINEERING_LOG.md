# AI Engineering Log

How AI was used in this project: I broke the work into small tasks and
gave AI one task at a time with context and constraints (stack, package
names, what must not change). Every generated file was added by hand,
compiled, and verified before moving to the next one - either by
running the app, running the tests, or calling the API with curl and
Postman. Each AI output was tracked as accepted, edited, or rejected,
with the reason recorded below. Several outputs were wrong or
incomplete; the interesting entries show what was caught and how.

AI was also used for debugging, not only generation: when something
failed, I collected the evidence (logs, dependency lists, test
reports, stack traces) and AI helped interpret it and locate the root
cause - which was then confirmed against official documentation before
any fix was applied.

## How tasks were defined

Each task given to AI carried intent, technical context, constraints
and acceptance criteria. Example (the short code generator):

    Task: implement short-code generation.
    Context: Java 17 / Spring Boot 4 / MySQL; UrlService owns creation;
      settings come from ShortenerProperties.
    Constraints:
      - 7 characters, URL-safe (Base62) only
      - MySQL has a UNIQUE constraint on short_code
      - retry on collision, maximum 3 tries, then fail clearly
      - do not change controller or repository APIs
    Acceptance criteria:
      - codes are exactly 7 URL-safe characters
      - collisions retried, explicit exception after max retries
      - unit tests cover generation and the collision path

## Quality gates and oversight

Rules applied to every change before it was accepted:

- compile after every file; no moving on with a broken build
- full test suite green before every commit
- manual curl/Postman check for every feature on top of the tests
- ddl-auto=validate as an automatic schema gate at startup
- secure AI usage: no credentials, secrets, production data, or
  proprietary customer information were included in prompts; generated
  code was reviewed for security-sensitive behavior before acceptance
- human approval: schema migrations, rollback operations, and
  compatibility-breaking changes required explicit engineer review and
  approval before being applied

## Summary of AI contributions and engineer actions

| Task | AI contribution | Engineer action | Status |
|------|-----------------|-----------------|--------|
| Project skeleton, Flyway setup | suggested flyway-core + flyway-mysql dependencies | diagnosed missing Spring Boot 4 integration and replaced it with spring-boot-starter-flyway | Edited |
| Integration test setup | generated test with @AutoConfigureMockMvc | fixed the package and added spring-boot-starter-webmvc-test | Edited |
| Short code generator | generated Base62 + SecureRandom implementation | reviewed and verified with 4 unit tests | Accepted |
| Error handling | generated global exception handler | added missing-body 400 handling and a regression test | Edited |
| V3 analytics migration | generated one ALTER TABLE with two columns | split it into two statements for MySQL/H2 compatibility | Edited |
| Entity change for expiration | changed constructor from 3 to 4 parameters | reviewed the compatibility trade-off and accepted the internal breaking change | Accepted with rationale |
| Reference implementation | included Kafka and Redis-style components | rejected as unnecessary for prototype scope | Rejected |

## Detailed entries

### Task: Flyway setup

**Intent:** versioned schema migrations instead of Hibernate ddl-auto.

**AI input:** asked for the Maven dependencies and configuration for
Flyway with MySQL on this Spring Boot project.

**AI output:** flyway-core and flyway-mysql dependencies plus
spring.flyway properties.

**Engineer review:** the app started and reported healthy, but the
migration never ran - no table, and not a single Flyway line in the
logs. Checked step by step: dependency present (mvnw dependency:list),
migration file present in target/classes, config file correct. So the
library was there but Spring never called it.

**Root cause:** Spring Boot 4 split its auto-configuration into
modules. flyway-core alone is no longer enough; the integration lives
in spring-boot-starter-flyway. Confirmed against the Spring Boot 4
migration documentation.

**Changes made:** replaced flyway-core with spring-boot-starter-flyway
(kept flyway-mysql).

**Validation:** restart showed "Migrating schema to version 1" and the
tables appeared in MySQL.

**Lesson:** AI suggested the Spring Boot 3 way. On a new major version,
silence in the logs is a signal to verify, not to assume.

### Task: Short code generator

**Intent:** generate compact, URL-safe short codes.

**AI output:** Base62 generation using SecureRandom.

**Engineer review:** verified the character set, configured length,
absence of persistence coupling, and that collision handling remained
in the service/database layer.

**Decision:** accepted.

**Validation:** unit tests verified configured length, Base62-only
characters, multiple generated values, and alternate configured lengths.

### Task: Integration tests

**Intent:** full Spring MVC pipeline tests with MockMvc against H2.

**AI output:** a test class importing
org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc.

**Engineer review:** did not compile - package does not exist.

**Root cause:** same Spring Boot 4 modularization. The annotation moved
to org.springframework.boot.webmvc.test.autoconfigure and requires the
spring-boot-starter-webmvc-test dependency.

**Changes made:** added the test starter, fixed the import.

**Validation:** all integration tests compile and pass.

### Task: Error handling

**Intent:** one JSON error shape for the whole API.

**AI output:** GlobalExceptionHandler covering our custom exceptions,
validation errors, and a catch-all that logs details but shows the user
only a generic message.

**Engineer review:** accepted the design, then tested edge cases by
hand in Postman. Sending a POST with no body at all returned 500 with
the generic message. A missing body is a client mistake and should be
400.

**Changes made:** added a handler for HttpMessageNotReadableException
returning 400 "Request body is missing or not valid JSON", plus a
regression test so the case stays covered.

**Lesson:** manual exploratory testing finds cases nobody wrote unit
tests for.

### Task: V3 analytics migration

**Intent:** add click_count and last_accessed_at columns.

**AI output:** one ALTER TABLE statement adding both columns.

**Engineer review:** applied fine on MySQL, application worked. Then
./mvnw test failed with 6 errors: the test context could not start
because H2 rejected the SQL - H2 does not accept two ADD COLUMN in one
ALTER TABLE, MySQL does.

**Changes made:** split into two ALTER TABLE statements. Because the
broken migration had already been applied only to the local
development database, I reset the local migration state by dropping the
new columns and removing the V3 Flyway history entry before reapplying
the corrected migration. This would not be an acceptable production
migration strategy; production changes would be fixed forward with a
new migration.

**Validation:** app starts on MySQL, all tests pass on H2.

**Lesson learned the hard way:** the local reset dropped the columns
and with them the click counts collected so far. Dropping a column
deletes its data - a first-hand demonstration of why applied migrations
are never edited or rolled back in production.

### Task: Constructor change for expiration

**Intent:** add expiresAt to the UrlMapping entity.

**What happened:** the change replaced the 3-parameter constructor with
a 4-parameter one, which broke compilation in every caller. I asked why
we did not keep both constructors for backward compatibility.

**Discussion result:** keeping the old constructor is right for a
shared library where other teams call your code. Here all callers are
inside this one project (there were three), so updating them in the
same commit is cleaner - and the compile errors acted as a checklist of
every place that needed to think about expiration. A convenience
constructor would let future code silently ignore that the feature
exists.

**Decision:** break the constructor, update all callers. Documented in
DECISIONS.md.

### Task: scope of the prototype (rejected approach)

A reference implementation was available that included Kafka events and
Redis-style rate limiting. I decided not to copy or imitate it: that
scale of infrastructure is not justified for a 2-3 day prototype, and
building from a copied design would remove my ownership of the
decisions. The simpler design (one service, one database, atomic SQL
update for counting) covers the actual requirements and every piece of
it can be explained and defended.

## Ownership statement

AI accelerated implementation, debugging, and review preparation. The
task breakdown, architecture, accept/edit/reject decisions, corrective
changes, validation, and final production-readiness judgment remained
under engineer ownership.