# ADR 0004 — Pluggable authentication with a uniform security context

**Status:** Accepted

## Context

Different applications authenticate differently: some need local DB accounts, some integrate a
corporate IdP via OIDC, some both. Binding the platform to a single mechanism would force every
application into it and leak the choice into business code (architecture §5.5).

## Decision

Expose a **pluggable authentication abstraction** with a **uniform security context**:

- `qavo.security.strategy` selects `local`, `oidc`, or `hybrid`.
- The local DB baseline (users/roles/permissions, BCrypt, Flyway schema, `UserDetailsService`,
  `AuthenticationManager`) ships out of the box and activates unless the strategy is pure `oidc`.
- The OIDC resource-server path activates when the OAuth2 dependency is present and an issuer is
  configured; it validates JWTs and maps a configurable claim to authorities.
- Business code depends only on `SecurityContextAccessor` / `AuthenticatedPrincipal`, which present
  the same shape (id, username, roles, permissions, attributes) regardless of strategy.
- Authorization is uniform and declarative: URL rules plus method-level `@PreAuthorize` over the
  role/permission model.

User-facing flows (login, registration) are separate, optional **plugins**, not part of the core
security strategy.

## Consequences

**Positive**
- Swapping the authentication strategy never touches business code.
- Secure-by-default headers/CORS/stateless sessions apply across all strategies.
- OIDC-only applications can exclude JPA entirely; local-only applications need no OAuth2.

**Negative**
- The baseline principal adapter leaves the attributes map empty to stay classloader-safe without
  the optional OAuth2 dependency; richer claim surfacing needs a dedicated accessor (roadmap).
- Local login token issuance is not yet implemented (see capabilities matrix / roadmap).

## Alternatives considered

- **Hardcode a single IdP.** Rejected: not reusable across applications with different needs.
- **Expose Spring Security types directly to applications.** Rejected: couples business code to the
  framework and the active strategy.
