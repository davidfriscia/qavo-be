# Qavo Backend — Capabilities Matrix

Status of each backend concern at version `0.0.0-SNAPSHOT`.

**Legend:** ✅ Implemented · 🟡 Partial · ⛔ Planned (not yet started) · ⬜ Out of scope (deliberate)

| # | Concern | Status | Maturity | What exists today | Missing / planned |
|---|---|---|---|---|---|
| 1 | Auto-configuration infrastructure | ✅ | Stable | `@AutoConfiguration` classes per module, `AutoConfiguration.imports`, `EnvironmentPostProcessor` defaults, conditional activation (`@ConditionalOnClass/Property/Bean/Expression`) | Configuration metadata polish |
| 2 | REST conventions | ✅ | Stable | `/api/v1` base via `ApiConventions`, path-based versioning, `PagedResponse` envelope, standard `page/size/sort` | `Deprecation`/`Sunset` header automation |
| 3 | Error handling (RFC 9457) | ✅ | Stable | `ProblemDetailFactory`, `CoreProblemType`, exception hierarchy, global `@RestControllerAdvice`, `traceId` correlation | Per-type documentation URIs hosting |
| 4 | Security — abstraction | ✅ | Beta | Pluggable strategy enum, uniform `SecurityContextAccessor`, `AuthenticatedPrincipal`, role/permission split, method security | — |
| 4a | Security — local auth | 🟡 | Beta | JPA user/role/permission model, `UserDetailsService`, BCrypt, Flyway baseline, `AuthenticationManager` | Account lockout, password reset, Argon2 option |
| 4b | Security — OIDC/OAuth2 | 🟡 | Beta | Resource-server config, `JwtDecoder` from issuer, claim→authority mapping, hybrid mode | Richer claim mapping, opaque-token support |
| 4c | Secure headers / TLS / CORS | ✅ | Stable | HSTS, CSP, frame/content-type options, referrer & permissions policy, `forward-headers-strategy`, centralized CORS | Per-route CSP overrides |
| 5 | Logging & observability | 🟡 | Beta | Structured JSON logging, enforced MDC (`traceId/appName/userId`), correlation filter, Actuator defaults, Micrometer, OTel bridge (optional) | Standard metric set dashboards, log-collection wiring |
| 6 | OpenAPI integration | ✅ | Stable | springdoc, info block from config, plugin-aware `x-qavo-plugins` extension, default group | Per-plugin grouped docs |
| 7 | Plugin infrastructure | ✅ | Stable | `QavoPlugin` SPI, `PluginRegistry`, conditional auto-config, public-path & migration contributions, login & registration plugins | Plugin lifecycle events |
| 8 | Database migrations | ✅ | Stable | Flyway, modular per-module locations merged by `QavoFlywayAutoConfiguration`, version banding convention | Baseline/repeatable migration helpers |
| 9 | Validation | ✅ | Stable | Bean Validation integration, `ValidationErrorMapper`, `StrongPassword` & `Slug` constraints, message bundle | More built-in constraints (fiscal code, VAT) |
| 10 | Testing infrastructure | ✅ | Beta | `AbstractPostgresIntegrationTest` (Testcontainers), `ProblemDetailAssertions`, module + reference-app tests | Slice-test helpers, plugin test harness |
| 11 | Feature flags | 🟡 | Alpha | `FeatureFlagService`, property-backed impl, `qavo.features.*` | Dynamic DB-backed flags, request-time evaluation |
| 12 | Resilience (timeout/retry/circuit breaker) | ⛔ | Planned | Resilience4j present in BOM | Qavo HTTP client wiring defaults |
| 13 | Auditing (created/modified) | ⛔ | Planned | — | Spring Data Auditing wired to security context |
| 14 | Internationalization (i18n) | ⛔ | Planned | Validation messages externalized | Central message bundle convention + locale resolution |
| 15 | Inter-service HTTP client | ⛔ | Planned | — | traceId + token propagation, resilience policies |
| 16 | Secrets management (Vault, etc.) | ⬜ | Out of scope | Env-var supplied secrets | Deferred by architecture §5.5 |
| 17 | Pluggable config providers | ⬜ | Out of scope | Spring profiles + env vars | Deferred by architecture §5.6 |
| 18 | Theming, mobile-first/responsive | ⬜ | Frontend | — | Lives in `qavo-fe` |

## Maturity definitions

- **Stable** — API and behavior are intended to be backward-compatible within the `0.x` line.
- **Beta** — functional and tested, but the surface may still change before `1.0.0`.
- **Alpha** — minimal implementation proving the contract; expect change.
- **Planned** — extension point and/or dependency present; behavior not yet implemented.

## Known limitations at `0.0.0-SNAPSHOT`

- Local login validates credentials but does **not** yet issue a signed bearer token, so the
  `/me` endpoint is only meaningful within an authenticated request (e.g. HTTP Basic). Token
  issuance is the top security roadmap item.
- Email verification is modeled (flag + token table) but the verification email is not sent.
- The standard operational metric set and Grafana dashboards are described but not yet bundled.
- Publishing (Maven Central signing, CI release) is not yet configured.
