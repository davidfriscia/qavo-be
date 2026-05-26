# ADR 0001 — Modular monolith with plugin capabilities

**Status:** Accepted

## Context

Qavo must deliver many cross-cutting capabilities (auth, registration, user management, storage,
notifications, …) to multiple applications. A key question is whether capabilities should be
**built into the core and toggled by configuration flags**, or **delivered as selectable plugins
that an application imports**. The platform's cardinal requirement is independent evolution and
long-term maintainability (architecture §1, §6.1).

## Decision

Adopt a **modular monolith** as the default application shape, and deliver every capability beyond
the small core as an **independently versioned plugin module**. Apply the split:

- **Modularity is the unit of distribution/existence** (build time): the application chooses which
  plugins to import. What is not imported does not exist in the app.
- **Configuration is the unit of activation/behavior** (runtime): once imported, a plugin's behavior
  is tuned via `qavo.<plugin>.*` properties.

Plugins auto-configure themselves through Spring Boot conditionals, contribute their own routes,
services, security rules, and Flyway migrations, and register a `QavoPlugin` descriptor.

## Consequences

**Positive**
- The core stays small, stable, and slow-moving; plugins evolve independently.
- Each application's footprint and attack surface are minimized — unused features carry no code,
  tables, or dependencies.
- A bug in a plugin is fixed and released without touching the core or unrelated applications.
- Module boundaries force clean APIs between core and plugins.

**Negative**
- More modules to manage and version than a single artifact.
- Requires discipline around shared resources (e.g. Flyway version banding — see ADR 0005).

## Alternatives considered

- **Everything in core, toggled by flags.** Rejected: the core becomes a "God module" that grows
  without bound, every release touches unrelated code, and applications ship features (and
  vulnerabilities) they never use.
- **Microservices from the start.** Rejected as the default: distributed-systems cost (tracing,
  latency, eventual consistency, independent pipelines) is unwarranted until a concrete operational
  reason justifies extracting a module (architecture §5.10).
