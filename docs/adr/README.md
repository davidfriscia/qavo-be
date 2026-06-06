# Architecture Decision Records

This directory records significant architectural decisions for the Qavo backend platform, in the
lightweight ADR format. Each record is immutable once accepted; a decision is changed by adding a
new ADR that supersedes the old one.

| ADR | Title | Status |
|---|---|---|
| [0001](0001-modular-monolith-and-plugins.md) | Modular monolith with plugin capabilities | Accepted |
| [0002](0002-starter-and-bom-distribution.md) | Spring Boot starter + BOM distribution | Accepted |
| [0003](0003-rfc9457-problem-details.md) | RFC 9457 Problem Details as the error contract | Accepted |
| [0004](0004-pluggable-authentication.md) | Pluggable authentication with a uniform security context | Accepted |
| [0005](0005-modular-flyway-migrations.md) | Modular Flyway migrations with version banding | Accepted |
| [0006](0006-local-jwt-token-issuance.md) | Local JWT token issuance with rotating refresh tokens | Accepted |
| [0007](0007-account-lockout.md) | Temporary account lockout after repeated failed logins | Accepted |
| [0008](0008-resilient-outbound-http-client.md) | Resilient outbound HTTP client | Accepted |
| [0009](0009-platform-jpa-auditing.md) | Platform JPA auditing wired to the security context | Accepted |
| [0010](0010-notifications-abstraction.md) | Pluggable notifications abstraction | Accepted |
| [0011](0011-email-verification-design.md) | Email verification design | Accepted |

## Format

Each ADR contains: **Status**, **Context**, **Decision**, **Consequences** (positive and negative),
and **Alternatives considered**.
