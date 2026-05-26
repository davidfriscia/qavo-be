# ADR 0005 — Modular Flyway migrations with version banding

**Status:** Accepted

## Context

Plugins own their own database tables and must ship the migrations that create them, so that
adding/removing a plugin adds/removes its schema (architecture §6, §8). Flyway, however, applies all
migrations into a single shared history and requires globally unique version numbers across all
locations.

## Decision

- Each module ships migrations under its **own reserved classpath location**
  (`classpath:db/qavo/<module>`, kept outside the application's `db/migration` tree so Flyway does
  not treat it as a redundant sub-location) and publishes a `MigrationLocation` bean.
- `QavoFlywayAutoConfiguration` discovers every `MigrationLocation` bean and **merges** the locations
  with whatever the application configured, via a `FlywayConfigurationCustomizer` — so plugin
  migrations are found and run automatically, with no per-application wiring.
- A **version-banding convention** keeps versions globally unique: the platform reserves
  `V0001`–`V0099`; applications start at `V0100`. Each plugin takes a disjoint sub-range
  (e.g. security `V0001+`, registration `V0010+`).

## Consequences

**Positive**
- Plugins are truly self-contained, including their schema.
- No central registry of migrations to maintain; discovery is automatic.
- Removing a plugin removes its migration location (its tables are no longer its concern).

**Negative**
- Version banding is a convention, not enforced by tooling — collisions are possible if ignored.
  A build-time check is on the roadmap.
- Dropping a plugin does not drop already-applied tables; data cleanup is an operational decision.

## Alternatives considered

- **Single shared migration folder.** Rejected: couples plugin schema to the core and breaks
  independent evolution.
- **Separate Flyway schema histories per module.** Rejected as over-engineered for the current
  scale; revisit if cross-module version coordination becomes painful.
