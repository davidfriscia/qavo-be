# ADR 0006 — Local JWT token issuance with rotating refresh tokens

**Status:** Accepted

## Context

The `qavo-auth-login` plugin authenticated credentials against the local user store but did not
issue any bearer token, so a successful `POST /api/v1/auth/login` returned an opaque
`AuthenticatedUserView` and downstream calls had no way to prove identity to other endpoints. The
platform already supported validating JWTs issued by an external identity provider via the OIDC
strategy (architecture §6.2.1); what was missing was the **issuance** side for the local strategy.

For Qavo to be a practical default backend for greenfield applications — and to honor the
"works first, integrates later" promise — local login must produce a verifiable bearer credential
that a SPA, mobile client, or downstream service can carry without an external IdP in the loop.

## Decision

Implement local token issuance entirely within `qavo-auth-login`, with the following choices:

- **Library:** JJWT 0.12.6 for signing access tokens. Selected over Nimbus-only for its tiny
  surface and clean api/impl/jackson split, and pinned in `qavo-bom`. Validation reuses the
  Nimbus decoder already pulled in by `spring-boot-starter-oauth2-resource-server`, so adding
  JJWT does not duplicate parsing logic on the resource-server side.
- **Algorithm:** HS256 with a Base64-encoded secret of at least 32 bytes. Symmetric keys keep
  the local strategy a single deployable unit; asymmetric/JWKS support is deferred to a future
  ADR if and when external token consumers materialize.
- **Claims:** `sub` (principal id), `iss`, `aud`, `iat`, `exp`, `jti`, `roles[]`,
  `preferred_username`. `roles` is the configurable authorities claim, consistent with the
  existing OIDC mapping convention.
- **Refresh tokens are opaque, random, and DB-backed.** The plugin generates 32 random bytes,
  hands the Base64URL-encoded value to the client, and stores **only** the SHA-256 hash. Tokens
  are rotated on every refresh: the presented token is revoked atomically and a new one is
  issued. Reuse of a revoked token returns `401`.
- **Logout** revokes every active refresh token for the authenticated user via a single bulk
  update, so a compromised access token expires within the access TTL and refresh is impossible.
- **Configuration is opt-out, not opt-in for activation,** but **opt-in for secrets.** An
  `EnvironmentPostProcessor` fails fast at startup with a clear remediation message if the
  plugin is on the classpath but `qavo.security.local.jwt.secret` is missing or too short.
- **Hybrid strategy** wires both the local and external decoders behind a composite that peeks
  the unverified `iss` claim and delegates to the matching decoder, so applications can accept
  both kinds of token without changing controller code.

## Consequences

**Positive**

- Local login becomes self-sufficient: an application can ship with username/password auth and
  a fully functional bearer-token flow without integrating an IdP.
- Refresh-token hashing means a database leak does not yield usable credentials.
- Rotation + reuse detection give a baseline for detecting stolen refresh tokens.
- The hybrid strategy gives consumers a graceful migration path from local credentials to an
  external IdP without forking the application.

**Negative**

- The platform now owns secret-management guidance: applications must set
  `qavo.security.local.jwt.secret` from a secret store, not a checked-in file.
- A second source of truth (JJWT for signing, Nimbus for validation) means version updates must
  be considered jointly, though the surface JJWT touches is small.
- Refresh tokens occupy database rows; a future migration may add a background sweeper for
  expired entries.

## Alternatives considered

- **Nimbus-only signing.** Rejected for ergonomics — JJWT's builder API is materially clearer at
  the call site, and the dependency footprint is comparable.
- **Stateless refresh tokens (JWTs as refresh tokens).** Rejected: revocation would require a
  deny-list with the same storage cost and no recoverable benefit.
- **Storing refresh tokens in plaintext.** Rejected for the obvious reason.
- **No refresh tokens; client re-authenticates.** Rejected because forcing password re-entry on
  every short-lived access-token expiry is hostile to the user experience and pushes
  applications to lengthen access-token TTLs, weakening the security posture.
