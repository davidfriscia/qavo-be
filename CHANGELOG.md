# Changelog

All notable changes to the Qavo backend platform are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [0.0.1-SNAPSHOT]

### Added
- **Local JWT issuance** (`qavo-auth-login`): `POST /api/v1/auth/login` returns an access token
  and a rotating refresh token; `POST /api/v1/auth/refresh` rotates the refresh token;
  `POST /api/v1/auth/logout` revokes it. Signing key validated at startup via an
  `EnvironmentPostProcessor`. JJWT 0.12.x. See [ADR 0006](docs/adr/0006-local-jwt-token-issuance.md).
- **Temporary account lockout** (`qavo-security`): per-user failed-attempt counter, lock window
  with `unlocksAt` surfaced as RFC 9457 `423 Locked`. `QavoException` gains a
  `getProblemProperties()` extension hook so domain exceptions can contribute Problem-Details
  fields. See [ADR 0007](docs/adr/0007-account-lockout.md).
- **`qavo-resilience` module**: `QavoHttpClient` over Spring `RestClient` instrumented with
  Resilience4j retry + circuit breaker per declared backend, plus `X-Trace-Id` propagation via
  `TraceContext`. Declarative client configuration under `qavo.resilience.http.clients.*`.
  WireMock pinned in the BOM for resilience tests. See
  [ADR 0008](docs/adr/0008-resilient-outbound-http-client.md).
- **`qavo-auditing` module**: `AuditableEntity` `@MappedSuperclass` and `QavoAuditorAware` wired
  to the platform's `SecurityContextAccessor`; auto-enabled JPA auditing. See
  [ADR 0009](docs/adr/0009-platform-jpa-auditing.md).
- GitHub Actions CI (`.github/workflows/ci.yml`): JDK 21 Temurin, Maven cache, `mvn clean verify`
  on push to `main` and pull requests.

### Changed
- Version bumped from `0.0.0-SNAPSHOT` to `0.0.1-SNAPSHOT`.
- `LoginController` response shape replaced with `LoginResponse` (access/refresh tokens + user).

## [0.0.0-SNAPSHOT]

### Added
- Initial platform foundation at `0.0.0-SNAPSHOT`:
  - `qavo-bom`, `qavo-core`, `qavo-observability`, `qavo-validation`, `qavo-security`,
    `qavo-openapi`, `qavo-starter-web`.
  - Authentication plugins: `qavo-auth-login`, `qavo-auth-registration`.
  - `qavo-test-support` and a runnable `qavo-reference-app`.
- RFC 9457 Problem Details error contract with `traceId` correlation.
- Pluggable authentication (local DB baseline + OIDC resource server), secure-by-default headers,
  centralized CORS, method security.
- Structured JSON logging with enforced MDC trace propagation.
- Plugin SPI/registry and modular Flyway migration discovery with version banding.
- springdoc OpenAPI integration with plugin-aware inventory.
- Bean Validation integration with reusable `StrongPassword` and `Slug` constraints.

See [docs/capabilities-matrix.md](docs/capabilities-matrix.md) for per-concern status and
[docs/roadmap.md](docs/roadmap.md) for what is planned.

[Unreleased]: https://github.com/davidfriscia/qavo-be
[0.0.1-SNAPSHOT]: https://github.com/davidfriscia/qavo-be
[0.0.0-SNAPSHOT]: https://github.com/davidfriscia/qavo-be
