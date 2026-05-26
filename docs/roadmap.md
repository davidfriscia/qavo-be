# Qavo Backend — Roadmap

Prioritized TODO roadmap for the backend platform, separated by current state. Priorities:
**P0** (foundational/blocking 1.0), **P1** (important), **P2** (nice to have).

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

## Partially implemented

| Item | State | Next step | Priority |
|---|---|---|---|
| Local login token issuance | Credentials validated; no token issued | Issue signed JWT (or opaque token) + refresh flow | **P0** |
| Email verification | Flag + token table modeled | Send verification email; consume token endpoint | P1 |
| Observability metric set | Micrometer + Actuator wired | Bundle standard metrics + Grafana dashboards | P1 |
| OIDC claim mapping | Single configurable claim | Nested/array claims, multiple providers, opaque tokens | P1 |
| Feature flags | Static property-backed | Request-time, DB-backed dynamic flags | P2 |

## Planned (not started)

### Platform hardening — **P0/P1**
- Resilience4j-backed Qavo HTTP client: timeout + retry (backoff/jitter) + circuit breaker defaults
  (architecture §5.9). **P0**
- Spring Data Auditing wired to the security context (`createdBy/At`, `lastModifiedBy/At`). **P1**
- `Deprecation`/`Sunset` header automation for versioned APIs (RFC 8594). **P1**
- Inter-service contract: `traceId` + auth-token propagation, service-account tokens. **P1**

### Future modules — **P1/P2**
- `qavo-user-mgmt` plugin (admin user/role management console API).
- `qavo-notifications` plugin (email/notification dispatch — unblocks email verification).
- `qavo-storage` plugin (file storage abstraction).
- `qavo-audit` plugin (audit log query API).
- `qavo-i18n` support (central message bundle convention + locale resolution).

### Operational improvements — **P1**
- CI on pull requests (build, test, static analysis, dependency scanning).
- Release pipeline: GPG-signed artifacts to Maven Central, tag-triggered.
- Configuration metadata (`spring-configuration-metadata.json`) completeness for all `qavo.*`.
- Maven Wrapper binary committed; reproducible builds.

### Security improvements — **P0/P1**
- Account lockout / brute-force protection on local login. **P0**
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

- The login plugin's value is limited until token issuance lands; treat `auth-login` as **beta**.
- Modular Flyway version banding is a convention, not enforced — consider a build-time check.
- The security context attribute map is empty in the baseline adapter; OIDC claim surfacing needs a
  dedicated accessor when richer attributes are required.
