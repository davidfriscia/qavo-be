# Changelog

All notable changes to the Qavo backend platform are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [0.0.3-SNAPSHOT]

### Added
- **Registration capacity cap** (`qavo-auth-registration`): opt-in rolling-window soft limit on
  self-service sign-up. New `RegistrationCapService` SPI in `qavo-core` (with the
  `DatabaseRegistrationCapService` default implementation and a `NoOpRegistrationCapService`
  fallback). When the configured maximum is reached within the rolling window,
  `POST /api/v1/auth/register` responds with `503 Service Unavailable`, sets the `Retry-After`
  header, and emits an RFC 9457 body carrying `opensAt` (ISO-8601 UTC) and `retryAfter`
  (seconds) extension members. A new public read-only endpoint
  `GET /api/v1/auth/registration-status` always returns `200 OK` with the same fields and
  `Cache-Control: no-store`. Configuration lives under `qavo.auth.registration.cap.*`
  (`enabled`, `max-registrations`, `window`, `include-unverified`) with fail-fast validation at
  startup. Tagged Micrometer metrics: `qavo.registration.cap.check{result=allowed|rejected}`,
  `qavo.registration.cap.current_count`, `qavo.registration.cap.utilization`. New RFC 9457
  problem type: `registration-cap-exceeded`. New Flyway migration
  `V0011__qavo_registration_events.sql`. See
  [ADR 0012](docs/adr/0012-registration-capacity-cap.md).
- **`QavoException.getResponseHeaders()`**: extension hook that lets domain exceptions
  contribute HTTP response headers (e.g. `Retry-After`) alongside the RFC 9457 body
  extensions returned by `getProblemProperties()`. The `GlobalExceptionHandler` merges them
  into every Problem Details response.

### Changed
- Version bumped from `0.0.2-SNAPSHOT` to `0.0.3-SNAPSHOT` across the reactor, BOM, and plugin
  descriptors.
- `qavo-bom` (transitively) covers the optional `micrometer-core` dependency now exposed by
  the registration plugin for its cap metrics.

## [0.0.2-SNAPSHOT]

### Added
- **`qavo-notifications` module**: a pluggable notification layer comprising the
  `NotificationDispatcher` facade, the `NotificationService` SPI, and three built-in providers
  (`EMAIL` via Spring `JavaMailSender`, `TELEGRAM` via `QavoHttpClient`, `NONE` no-op). Failures
  are returned, never thrown — business operations are never blocked by an unreachable
  notification backend. Configuration sits under `qavo.notifications.*`. Tagged metrics:
  `qavo.notifications.sent{channel,status}` and `qavo.notifications.providers.registered`. See
  [ADR 0010](docs/adr/0010-notifications-abstraction.md).
- **Email verification flow** (`qavo-auth-registration`): self-service registration now optionally
  issues a single-use, SHA-256-hashed token persisted in `qavo_email_verification_tokens` and
  dispatches a verification email via the new `NotificationDispatcher`. The user verifies via
  `GET /api/v1/auth/verify-email?token={raw}` and may request a fresh email via
  `POST /api/v1/auth/verify-email/resend` (anti-enumeration: always 202, with a per-hour rate
  limit returning 429 + `retryAfterSeconds`). New RFC 9457 problem types:
  `email-not-verified`, `invalid-verification-token`, `verification-token-expired`,
  `verification-token-already-used`, `resend-rate-limited`. See
  [ADR 0011](docs/adr/0011-email-verification-design.md).
- **Email-verified login guard** (`qavo-auth-login`): when
  `qavo.auth.registration.email-verification.require-verified-email-to-login=true`, credential
  authentication of an unverified user returns 403 `email-not-verified` (after successful
  credential and lockout checks, so it cannot leak account existence).

### Changed
- Version bumped from `0.0.1-SNAPSHOT` to `0.0.2-SNAPSHOT` across the reactor, BOM, and plugin
  descriptors.
- `qavo-bom` now manages `qavo-notifications` and the GreenMail 2.1.0 test dependency.

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
[0.0.3-SNAPSHOT]: https://github.com/davidfriscia/qavo-be
[0.0.2-SNAPSHOT]: https://github.com/davidfriscia/qavo-be
[0.0.1-SNAPSHOT]: https://github.com/davidfriscia/qavo-be
[0.0.0-SNAPSHOT]: https://github.com/davidfriscia/qavo-be
