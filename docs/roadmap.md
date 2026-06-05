# Qavo Backend — Roadmap

Prioritized TODO roadmap for the backend platform, separated by current state. Priorities:
**P0** (foundational/blocking 1.0), **P1** (important), **P2** (nice to have).

Current version: `0.0.1-SNAPSHOT`. The latest sprint closed the five P0 items listed below
under *Recently completed*.

---

## Implemented

These are in place and consistent (see the [capabilities matrix](capabilities-matrix.md)):

- Multi-module Maven structure with BOM-driven dependency management.
- Auto-configuration infrastructure (conditional, override-friendly).
- RFC 9457 error model, exception hierarchy, global handler, `traceId` correlation.
- Pagination contract (`PagedResponse`, `page/size/sort`).
- Pluggable security abstraction, local-auth baseline, OIDC resource-server hooks, secure headers,
  CORS, method security.
- Structured JSON logging with enforced MDC trace propagation.
- springdoc OpenAPI with plugin-aware inventory.
- Plugin SPI + registry; login and registration plugins.
- Modular Flyway migration discovery with version banding.
- Bean Validation integration with reusable constraints.
- Testcontainers-based integration-test support.
- Runnable reference application.

### Recently completed (0.0.1-SNAPSHOT)

- **Local JWT token issuance** with rotating refresh tokens — `POST /api/v1/auth/login`,
  `/refresh`, `/logout`; signing key validated at startup; JJWT 0.12.x.
  See [ADR 0006](adr/0006-local-jwt-token-issuance.md). **(was P0)**
- **Account lockout / brute-force protection** on local login — time-bounded automatic lockout
  surfaced as RFC 9457 `423 Locked` with `unlocksAt`. See
  [ADR 0007](adr/0007-account-lockout.md). **(was P0)**
- **`qavo-resilience` module** — `QavoHttpClient` over Spring `RestClient` with Resilience4j
  retry + circuit breaker and `X-Trace-Id` propagation per declared backend. See
  [ADR 0008](adr/0008-resilient-outbound-http-client.md). **(was P0)**
- **`qavo-auditing` module** — `AuditableEntity` `@MappedSuperclass` + `QavoAuditorAware` wired
  to the platform `SecurityContextAccessor`; auto-enabled JPA auditing. See
  [ADR 0009](adr/0009-platform-jpa-auditing.md). **(was P0)**
- **GitHub Actions CI** — `.github/workflows/ci.yml` runs `mvn clean verify` on Temurin 21 for
  pushes to `main` and every pull request. **(was P0)**

## Partially implemented

| Item | State | Next step | Priority |
|---|---|---|---|
| Email verification | Flag + token table modeled | Send verification email; consume token endpoint | P1 |
| Observability metric set | Micrometer + Actuator wired | Bundle standard metrics + Grafana dashboards | P1 |
| OIDC claim mapping | Single configurable claim | Nested/array claims, multiple providers, opaque tokens | P1 |
| Auditing rollout | `AuditableEntity` available; platform tables not yet migrated onto it | Retrofit `QavoUser` / `QavoRole` / `RefreshToken` (column rename + Flyway plan) | P1 |
| Feature flags | Static property-backed | Request-time, DB-backed dynamic flags | P2 |

## Planned (not started)

### Platform hardening — **P1**
- `Deprecation`/`Sunset` header automation for versioned APIs (RFC 8594). **P1**
- Inter-service contract polish: bearer-token relay helper and service-account tokens on
  `QavoHttpClient`. **P1**
- Bulkhead and rate-limiter helpers on `qavo-resilience` (Resilience4j supports both). **P1**
- Reactive `WebClient`-backed variant of `QavoHttpClient` registered side-by-side. **P2**

### Future modules — **P1/P2**
- `qavo-user-mgmt` plugin (admin user/role management console API).
- `qavo-notifications` plugin (email/notification dispatch — unblocks email verification).
- `qavo-storage` plugin (file storage abstraction).
- `qavo-audit` plugin (audit log query API on top of `qavo-auditing` columns).
- `qavo-i18n` support (central message bundle convention + locale resolution).

### Operational improvements — **P1**
- Release pipeline: GPG-signed artifacts to Maven Central, tag-triggered.
- Configuration metadata (`spring-configuration-metadata.json`) completeness for all `qavo.*`.
- Maven Wrapper binary committed; reproducible builds.
- Static analysis + dependency scanning steps in CI.

### Security improvements — **P1/P2**
- Password reset flow. **P1**
- Argon2 password-encoder option alongside BCrypt. **P2**
- Per-route Content-Security-Policy overrides. **P2**
- Security headers conformance test suite. **P1**

### Cloud-native roadmap — **P2**
- Optimized container images (layered JAR; optional GraalVM native image).
- Kubernetes-ready probes documented end to end (liveness/readiness already exposed).
- OpenTelemetry exporter presets (OTLP) and sampling guidance per environment.
- Optional pluggable config/secret providers (currently out of scope — revisit on demand).

## Architectural gaps to watch

- Modular Flyway version banding is a convention, not enforced — consider a build-time check.
- The security context attribute map is empty in the baseline adapter; OIDC claim surfacing needs
  a dedicated accessor when richer attributes are required.
- Platform entities still carry hand-rolled `created_at` columns instead of extending
  `AuditableEntity` — the migration was deliberately deferred
  (see [ADR 0009](adr/0009-platform-jpa-auditing.md)).
