# Qavo Backend — Capabilities Matrix

Status of each backend concern at version `0.0.2-SNAPSHOT`.

**Legend:** ✅ Implemented · 🟡 Partial · ⛔ Planned (not yet started) · ⬜ Out of scope (deliberate)

| # | Concern | Status | Maturity | What exists today | Missing / planned |
|---|---|---|---|---|---|
| 1 | Auto-configuration infrastructure | ✅ | Stable | `@AutoConfiguration` classes per module, `AutoConfiguration.imports`, `EnvironmentPostProcessor` defaults, conditional activation (`@ConditionalOnClass/Property/Bean/Expression`) | Configuration metadata polish |
| 2 | REST conventions | ✅ | Stable | `/api/v1` base via `ApiConventions`, path-based versioning, `PagedResponse` envelope, standard `page/size/sort` | `Deprecation`/`Sunset` header automation |
| 3 | Error handling (RFC 9457) | ✅ | Stable | `ProblemDetailFactory`, `CoreProblemType`, exception hierarchy, global `@RestControllerAdvice`, `traceId` correlation | Per-type documentation URIs hosting |
| 4 | Security — abstraction | ✅ | Beta | Pluggable strategy enum, uniform `SecurityContextAccessor`, `AuthenticatedPrincipal`, role/permission split, method security | — |
| 4a | Security — local auth | ✅ | Beta | JPA user/role/permission model, `UserDetailsService`, BCrypt, Flyway baseline, `AuthenticationManager`, **local JWT issuance with rotating refresh tokens** (JJWT 0.12.x, `POST /api/v1/auth/login` / `/refresh` / `/logout`), **temporary account lockout** after repeated failures (HTTP 423 with `unlocksAt`), **email-verification login guard** (opt-in via `qavo.auth.registration.email-verification.require-verified-email-to-login`) | Password reset, Argon2 option |
| 4b | Security — OIDC/OAuth2 | 🟡 | Beta | Resource-server config, `JwtDecoder` from issuer, claim→authority mapping, hybrid mode | Richer claim mapping, opaque-token support |
| 4c | Secure headers / TLS / CORS | ✅ | Stable | HSTS, CSP, frame/content-type options, referrer & permissions policy, `forward-headers-strategy`, centralized CORS | Per-route CSP overrides |
| 5 | Logging & observability | 🟡 | Beta | Structured JSON logging, enforced MDC (`traceId/appName/userId`), correlation filter, Actuator defaults, Micrometer, OTel bridge (optional) | Standard metric set dashboards, log-collection wiring |
| 6 | OpenAPI integration | ✅ | Stable | springdoc, info block from config, plugin-aware `x-qavo-plugins` extension, default group | Per-plugin grouped docs |
| 7 | Plugin infrastructure | ✅ | Stable | `QavoPlugin` SPI, `PluginRegistry`, conditional auto-config, public-path & migration contributions, login & registration plugins | Plugin lifecycle events |
| 8 | Database migrations | ✅ | Stable | Flyway, modular per-module locations merged by `QavoFlywayAutoConfiguration`, version banding convention | Baseline/repeatable migration helpers |
| 9 | Validation | ✅ | Stable | Bean Validation integration, `ValidationErrorMapper`, `StrongPassword` & `Slug` constraints, message bundle | More built-in constraints (fiscal code, VAT) |
| 10 | Testing infrastructure | ✅ | Beta | `AbstractPostgresIntegrationTest` (Testcontainers), `ProblemDetailAssertions`, module + reference-app tests | Slice-test helpers, plugin test harness |
| 11 | Feature flags | 🟡 | Alpha | `FeatureFlagService`, property-backed impl, `qavo.features.*` | Dynamic DB-backed flags, request-time evaluation |
| 12 | Resilience (timeout/retry/circuit breaker) | ✅ | Beta | `qavo-resilience` module: `QavoHttpClient` over Spring `RestClient`, Resilience4j retry + circuit breaker per declared backend, configurable connect/read timeouts | Bulkhead/rate-limiter helpers, reactive variant |
| 13 | Auditing (created/modified) | ✅ | Beta | `qavo-auditing` module: `AuditableEntity` `@MappedSuperclass` with `created_at` / `last_modified_at` / `created_by` / `last_modified_by`, `QavoAuditorAware` reading the platform `SecurityContextAccessor`, autoconfigured `@EnableJpaAuditing` | Migration of existing platform entities (`QavoUser`, etc.) onto `AuditableEntity` |
| 13a | Notifications | ✅ | Beta | `qavo-notifications` module: `NotificationDispatcher` facade, EMAIL (`JavaMailSender`), TELEGRAM (`QavoHttpClient`), and NONE providers; fail-soft `NotificationResult`; metrics `qavo.notifications.sent{channel,status}` + `qavo.notifications.providers.registered` | SMS / web-push providers; templating engine |
| 13b | Email verification | ✅ | Beta | Registration plugin issues SHA-256-hashed single-use tokens, dispatches via `NotificationDispatcher`, exposes `GET /api/v1/auth/verify-email` + `POST /api/v1/auth/verify-email/resend` (anti-enumeration 202, per-hour rate limit), optional login guard | Configurable email body / template |
| 14 | Internationalization (i18n) | ⛔ | Planned | Validation messages externalized | Central message bundle convention + locale resolution |
| 15 | Inter-service HTTP client | ✅ | Beta | `QavoHttpClient` + `QavoHttpClientRegistry` (`qavo-resilience`): per-backend `RestClient` with traceId header propagation via `TraceContext` and per-client Resilience4j policies | Bearer-token relay helper, mTLS profile |
| 16 | Secrets management (Vault, etc.) | ⬜ | Out of scope | Env-var supplied secrets | Deferred by architecture §5.5 |
| 17 | Pluggable config providers | ⬜ | Out of scope | Spring profiles + env vars | Deferred by architecture §5.6 |
| 18 | Theming, mobile-first/responsive | ⬜ | Frontend | — | Lives in `qavo-fe` |

## Maturity definitions

- **Stable** — API and behavior are intended to be backward-compatible within the `0.x` line.
- **Beta** — functional and tested, but the surface may still change before `1.0.0`.
- **Alpha** — minimal implementation proving the contract; expect change.
- **Planned** — extension point and/or dependency present; behavior not yet implemented.

## Known limitations at `0.0.2-SNAPSHOT`

- Password reset and Argon2 hashing for the local strategy are still pending.
- The standard operational metric set and Grafana dashboards are described but not yet bundled.
- `AuditableEntity` is available but existing platform entities (`QavoUser`, `QavoRole`,
  `RefreshToken`) have not yet been migrated onto it — deliberately deferred so the new module
  ships as pure additive capability (see [ADR 0009](adr/0009-platform-jpa-auditing.md)).
- The `TELEGRAM` notification provider uses the public Bot API; outbound traffic to
  `api.telegram.org` must be reachable from the running application, and a `QavoHttpClient`
  named after `qavo.notifications.telegram.client-name` (default `telegram`) must be declared
  under `qavo.resilience.http.clients`.
- The verification email body is a plain text template baked into the registration plugin; a
  pluggable template/i18n hook is planned.
- CI runs `mvn clean verify` on every PR; release publishing (Maven Central signing) is not yet
  configured.
