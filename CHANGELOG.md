# Changelog

All notable changes to the Qavo backend platform are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to
[Semantic Versioning](https://semver.org/).

## [Unreleased]

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
