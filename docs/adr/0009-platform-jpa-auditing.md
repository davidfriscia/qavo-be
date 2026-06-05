# ADR 0009 — Platform JPA auditing wired to the security context

**Status:** Accepted

## Context

Every persistent record in a Qavo-based service eventually needs the four "who changed what when"
columns: created-at, last-modified-at, created-by, last-modified-by. Left to each team, this
either gets duplicated as ad-hoc columns on every entity (with drift in column names, types, and
update logic) or omitted entirely — and then becomes impossible to backfill once the audit need
is realized. The platform's promise of uniform cross-cutting infrastructure (architecture §5)
makes shipping a single, opinionated audit mechanism a baseline obligation, not an optional add.

## Decision

Introduce a new `qavo-auditing` module exposing **`AuditableEntity`** (a JPA
`@MappedSuperclass`) and a **`QavoAuditorAware`** that resolves the current auditor through the
strategy-independent `SecurityContextAccessor`.

- **Spring Data Auditing over a custom `@PrePersist`/`@PreUpdate` listener.** Spring Data
  Auditing already implements the lifecycle hooks, the `AuditorAware`/`DateTimeProvider` SPI,
  and the integration with the JPA `EntityListener` mechanism. Rolling our own would mean
  duplicating well-tested machinery for zero behavioural gain.
- **`String` auditor type holding the principal id.** Mapping `created_by`/`last_modified_by` as
  a JPA `@ManyToOne` to the user table would couple every auditable schema to local-auth
  internals and silently break for principals that come from OIDC (whose subjects do not exist
  as rows in `qavo_users`). Storing the id as a string keeps the audit columns valid regardless
  of which strategy was active when the row was written, at the cost of forcing a join when an
  operator wants the human-readable display name — an acceptable trade for a column that is
  usually read by humans during incident review, not by application queries.
- **`AuditorAware` resolved via `SecurityContextAccessor`.** This is the same abstraction the
  rest of the platform uses to read the current principal (ADR 0004); it means auditing works
  identically under local DB, OIDC, hybrid, or future strategies without per-strategy code paths.
- **System-principal fallback.** Writes that happen outside an authenticated request
  (`CommandLineRunner` data loaders, `@Scheduled` jobs, internal queue consumers) still need
  non-null audit columns. `qavo.auditing.system-principal` (default `"system"`) is recorded
  literally so audit queries can distinguish user-driven from system-driven changes.
- **Auto-config gated on `JpaRepository.class` and `EnableJpaAuditing.class`.** Services without
  Spring Data JPA pay no cost; the autoconfig becomes inert.
- **`@ConditionalOnMissingBean(AuditorAware.class)` on the auditor bean.** Apps that need a
  non-string auditor type (e.g. `AuditorAware<UUID>`) override the bean without the platform
  fighting them.
- **No retrofit of existing platform entities in this ADR.** `QavoUser`, `QavoRole`, and
  `RefreshToken` already carry hand-rolled `createdAt` columns; converting them to extend
  `AuditableEntity` is a behaviour-equivalent migration that would require renaming columns and
  introducing two Flyway migrations across two module bands. That work is intentionally deferred
  to its own changeset so this ADR can ship as pure additive capability with zero regression
  surface.

## Consequences

- New auditable entities in any Qavo-based service get four standard columns by extending one
  base class — no per-entity boilerplate.
- Audit columns survive auth-strategy changes; switching from local to OIDC does not invalidate
  existing audit history.
- Applications using `AuditorAware<UUID>` or similar still work — they declare their own bean
  and the platform stays out of the way.
- A follow-up changeset can migrate `qavo-security` and `qavo-auth-login` entities onto
  `AuditableEntity` once the column-rename Flyway plan is agreed; nothing in this ADR blocks it.
