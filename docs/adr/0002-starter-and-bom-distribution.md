# ADR 0002 — Spring Boot starter + BOM distribution

**Status:** Accepted

## Context

Applications need to inherit shared behavior and consistent dependency versions without manually
wiring configuration or managing the versions of many libraries. The platform must support
independent evolution: an application changes its shared behavior by bumping a single version
(architecture §2.1, §3.2).

## Decision

Distribute the platform as **Spring Boot starters** governed by a **Bill of Materials (`qavo-bom`)**:

- Each module exposes its behavior via `@AutoConfiguration` classes listed in
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- `qavo-starter-web` is the "always import" aggregator pulling the core modules and wiring the
  web-layer concerns.
- `qavo-bom` pins every Qavo module version plus the curated third-party versions. Applications
  import the BOM and declare modules without versions.
- All beans are `@ConditionalOnMissingBean` so applications can override any of them.

## Consequences

**Positive**
- Plug-and-play, convention-driven onboarding: import + minimal config = working baseline.
- Single-version upgrades; consistent transitive versions across applications.
- The same mechanism Spring itself uses — familiar to the ecosystem.

**Negative**
- Auto-configuration "magic" must be documented carefully (hence the configuration reference and
  capabilities matrix) to avoid surprise.
- Ordering and conditions require care (e.g. the security accessor overriding the core fallback).

## Alternatives considered

- **Plain shared library with manual `@Import`.** Rejected: shifts wiring burden to every
  application and undermines convention-over-configuration.
- **Gradle convention plugins.** Viable, but Maven + BOM was chosen for first-class BOM ergonomics
  (architecture §2.1).
