# Qavo Configuration Reference (`qavo.*`)

Every platform configuration property lives under the `qavo.*` namespace; application-specific
configuration lives under `app.*` (architecture §5.6). All properties have sensible, secure
defaults — override only what you need. Secrets are supplied via environment variables, never
hardcoded.

> This document is the human-readable counterpart to the generated
> `spring-configuration-metadata.json` (which provides IDE auto-completion).

---

## `qavo.api.*` — REST/OpenAPI conventions (`qavo-core`)

| Property | Type | Default | Description |
|---|---|---|---|
| `qavo.api.base-path` | String | `/api/v1` | Base path inherited by controllers. Path-based versioning is enforced. |
| `qavo.api.title` | String | `Qavo Application API` | OpenAPI document title. |
| `qavo.api.version` | String | `v1` | OpenAPI document version. |
| `qavo.api.description` | String | `API built on the Qavo platform.` | OpenAPI description. |
| `qavo.api.contact-name` | String | `Qavo` | OpenAPI contact name. |
| `qavo.api.contact-email` | String | `support@qavo.org` | OpenAPI contact email. |
| `qavo.api.license-name` | String | `MIT` | OpenAPI license name. |

## `qavo.error.*` — Problem Details (`qavo-core`)

| Property | Type | Default | Description |
|---|---|---|---|
| `qavo.error.base-uri` | String | `https://errors.qavo.org` | Base URI used to build the `type` member. |
| `qavo.error.include-stack-trace-detail` | boolean | `false` | Include exception message in `detail` for 5xx errors. Keep `false` in production. |

## `qavo.features.*` — Feature flags (`qavo-core`)

| Property | Type | Default | Description |
|---|---|---|---|
| `qavo.features.<name>` | boolean | `false` (absent) | Static feature flag, evaluated by `FeatureFlagService`. Example: `qavo.features.widget-export: true`. |

## `qavo.observability.*` — Logging & tracing (`qavo-observability`)

| Property | Type | Default | Description |
|---|---|---|---|
| `qavo.observability.application-name` | String | `qavo-app` | Logical name stamped as the `appName` MDC field and metric tag. |
| `qavo.observability.generate-trace-id-if-missing` | boolean | `true` | Generate a `traceId` when no W3C `traceparent` header is present. |
| `qavo.observability.response-trace-header` | String | `X-Trace-Id` | Response header echoing the resolved `traceId`. |

> The module also contributes secure Actuator/tracing defaults (endpoint exposure
> `health,info,metrics,prometheus`; health details when authorized; probes enabled; sampling 0.1;
> `server.forward-headers-strategy=framework`) as low-precedence defaults you can override.

## `qavo.security.*` — Security (`qavo-security`)

| Property | Type | Default | Description |
|---|---|---|---|
| `qavo.security.strategy` | enum | `local` | `local` \| `oidc` \| `hybrid`. |
| `qavo.security.public-paths` | List<String> | health/info, OpenAPI, Swagger | Ant patterns permitted without authentication. |

### `qavo.security.headers.*`

| Property | Type | Default | Description |
|---|---|---|---|
| `hsts-enabled` | boolean | `true` | Enable HSTS. |
| `hsts-max-age-seconds` | long | `31536000` | HSTS max-age (1 year). |
| `hsts-include-sub-domains` | boolean | `true` | Apply HSTS to subdomains. |
| `content-security-policy` | String | `default-src 'self'; frame-ancestors 'none'; object-src 'none'` | CSP directive. |
| `referrer-policy` | String | `strict-origin-when-cross-origin` | Referrer-Policy. |
| `permissions-policy` | String | `geolocation=(), microphone=(), camera=()` | Permissions-Policy. |

### `qavo.security.cors.*`

| Property | Type | Default | Description |
|---|---|---|---|
| `enabled` | boolean | `false` | Register a CORS policy. When false, same-origin only. |
| `allowed-origins` | List<String> | `[]` | Explicit allowed origins. |
| `allowed-methods` | List<String> | `GET,POST,PUT,PATCH,DELETE` | Allowed methods. |
| `allowed-headers` | List<String> | `*` | Allowed headers. |
| `allow-credentials` | boolean | `false` | Allow credentials. |

### `qavo.security.oidc.*`

| Property | Type | Default | Description |
|---|---|---|---|
| `issuer-uri` | String | — | OIDC issuer; required for `oidc`/`hybrid`. |
| `jwk-set-uri` | String | — | Explicit JWK set URI (otherwise discovered). |
| `authorities-claim` | String | `roles` | JWT claim carrying roles/authorities. |
| `authority-prefix` | String | `ROLE_` | Prefix applied to mapped authorities. |

### `qavo.security.local.jwt.*`

Active when `qavo.security.strategy` is `local` or `hybrid`. The signing secret is **required**;
the application fails to start if it is missing or shorter than 32 bytes after Base64 decoding.

| Property | Type | Default | Description |
|---|---|---|---|
| `secret` | String | — | Base64-encoded HS256 signing key (≥ 32 bytes). Required. Source from a secret manager — never check in. |
| `issuer` | String | `qavo` | Value placed in the `iss` claim and required on validation. |
| `audience` | String | `qavo-clients` | Value placed in the `aud` claim and required on validation. |
| `access-token-duration` | Duration | `PT30M` | Lifetime of issued access tokens. |
| `refresh-token-duration` | Duration | `P7D` | Lifetime of issued refresh tokens. |
| `authorities-claim` | String | `roles` | Claim name carrying granted authorities. |
| `authority-prefix` | String | `ROLE_` | Prefix applied to mapped authorities. |

### `qavo.security.local.lockout.*`

Active when `qavo.security.strategy` is `local` or `hybrid`. Counts consecutive failed local
logins per username and, on reaching the threshold, returns HTTP `423 Locked` with a Problem
Details body carrying an `unlocksAt` extension property (RFC 9457). Successful authentication
resets both the counter and any active lock.

| Property | Type | Default | Description |
|---|---|---|---|
| `enabled` | boolean | `true` | Enable temporary account lockout on repeated failures. |
| `max-attempts` | int | `5` | Failed-login attempts before the account is locked. |
| `duration` | Duration | `PT15M` | How long an account stays locked after the threshold is hit. |

## `qavo.auth.login.*` — Login plugin (`qavo-auth-login`)

When enabled, contributes `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`,
`POST /api/v1/auth/logout`, and `GET /api/v1/auth/me`. The first two are added to
`qavo.security.public-paths` automatically; `/logout` and `/me` require a bearer token. JWT
signing is controlled by [`qavo.security.local.jwt.*`](#qavosecuritylocaljwt) above.

| Property | Type | Default | Description |
|---|---|---|---|
| `qavo.auth.login.enabled` | boolean | `true` | Activate the login endpoints. |

## `qavo.auth.registration.*` — Registration plugin (`qavo-auth-registration`)

| Property | Type | Default | Description |
|---|---|---|---|
| `qavo.auth.registration.enabled` | boolean | `true` | Activate the registration plugin. |
| `qavo.auth.registration.self-service` | boolean | `true` | Allow public self-service sign-up. |
| `qavo.auth.registration.require-email-verification` | boolean | `false` | Require email verification before activation. |
| `qavo.auth.registration.default-role` | String | `USER` | Role granted to new users. |

## `qavo.resilience.http.*` — Resilient outbound HTTP client (`qavo-resilience`)

Declares the outbound backends fronted by `QavoHttpClient`. Each entry under `clients` produces
one client looked up from `QavoHttpClientRegistry` by name; retry and circuit-breaker policies
for that name are configured under the standard Resilience4j keys
(`resilience4j.retry.instances.<name>.*` and `resilience4j.circuitbreaker.instances.<name>.*`).
See [ADR 0008](adr/0008-resilient-outbound-http-client.md).

| Property | Type | Default | Description |
|---|---|---|---|
| `qavo.resilience.http.trace-header` | String | `X-Trace-Id` | Header used to forward the current trace ID on every outbound request. |
| `qavo.resilience.http.clients.<name>.base-url` | String | — | Base URL prefixed to every relative path used with this client. |
| `qavo.resilience.http.clients.<name>.connect-timeout` | Duration | `PT2S` | Time allowed to establish a TCP connection. |
| `qavo.resilience.http.clients.<name>.read-timeout` | Duration | `PT10S` | Time allowed to read a response after the connection is established. |

## `qavo.auditing.*` — Platform JPA auditing (`qavo-auditing`)

Activates Spring Data JPA Auditing across the application and resolves the current auditor
through the platform's `SecurityContextAccessor`. Entities opt in by extending
`org.qavo.auditing.AuditableEntity`. See [ADR 0009](adr/0009-platform-jpa-auditing.md).

| Property | Type | Default | Description |
|---|---|---|---|
| `qavo.auditing.enabled` | boolean | `true` | Master switch. When `false`, no auditing autoconfig is applied. |
| `qavo.auditing.system-principal` | String | `system` | Principal id recorded for writes that happen outside an authenticated request (scheduled jobs, data loaders). |
