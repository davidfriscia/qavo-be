# ADR 0011 — Email verification design

**Status:** Accepted (2026-Q1)

## Context

The registration plugin had a long-standing `emailVerified` flag on `QavoUser` but no way to
flip it. Verification needs to be:

- **Opt-in** — many applications run behind corporate SSO and never need it.
- **Plugin-local** — token storage and endpoints belong to the registration plugin; the platform
  must not assume verification is on.
- **Fail-soft on send** — an SMTP outage cannot break user creation.
- **Strict on consume** — a wrong/expired/used token must produce a precise RFC 9457 problem.
- **Anti-enumeration** — the resend endpoint must not reveal whether an email exists in the
  store.

## Decision

### Persistence

`qavo_email_verification_tokens` (V0010 in the registration plugin's migration band):

```sql
CREATE TABLE qavo_email_verification_tokens (
    token      VARCHAR(128) PRIMARY KEY,  -- SHA-256 hex of the raw token
    user_id    UUID         NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    consumed   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL
);
```

Only the SHA-256 digest of the raw token reaches the database — identical to the refresh-token
convention in the login plugin. The `consumed` column makes tokens single-use; the `created_at`
column backs the resend rate limit. No separate ledger is required.

### Flow

```text
register → save user (emailVerified=false)
         → EmailVerificationService.issueFor(user):
               raw   = random 32 bytes → URL-safe Base64 (no padding)
               hash  = sha256Hex(raw)
               row   = (hash, user.id, now + tokenDuration, consumed=false, now)
               link  = {baseUrl}/api/v1/auth/verify-email?token={raw}
               dispatcher.dispatch(NotificationRequest.email(...))   ← fail-soft

verify (GET /api/v1/auth/verify-email?token={raw}):
        row = find by sha256Hex(raw)
          ↳ missing       → InvalidVerificationTokenException             (400)
          ↳ consumed      → VerificationTokenAlreadyUsedException         (400)
          ↳ expired       → VerificationTokenExpiredException             (400)
        user.emailVerified = true
        row.consumed       = true
        → 200 {"status":"verified"}

resend (POST /api/v1/auth/verify-email/resend):
        ↳ unknown email or already-verified user  → silent 202 (anti-enumeration)
        ↳ count(tokens for user in last hour) ≥ resendMaxPerHour
              → ResendRateLimitedException (429 with retryAfterSeconds)
        else → issueFor(user); 202
```

### Login guard

Optional and off by default:

```yaml
qavo:
  auth:
    registration:
      email-verification:
        require-verified-email-to-login: true
```

When the flag is on, `LoginController` looks up the now-authenticated user and, if
`emailVerified=false`, throws `EmailNotVerifiedException` → 403
`type=email-not-verified`. The check runs **after** credential validation and the lockout check
so a wrong password still returns 401 (no account-existence leak).

### New RFC 9457 problem types

Added to `CoreProblemType` in `qavo-core`:

| code | HTTP | When |
|---|---|---|
| `email-not-verified` | 403 | Login guard rejects an unverified user |
| `invalid-verification-token` | 400 | Token unknown or blank |
| `verification-token-expired` | 400 | `expires_at <= now` |
| `verification-token-already-used` | 400 | `consumed=true` |
| `resend-rate-limited` | 429 | Per-hour ceiling exceeded; carries `retryAfterSeconds` extension |

## Consequences

**Positive**

- Single-use, hashed-at-rest verification tokens with explicit machine-readable failure modes.
- The flow degrades gracefully when no `NotificationDispatcher` is configured (token row is
  written, WARN is logged) so it remains testable in isolation.
- Login guard is a single boolean toggle — adoption is incremental.

**Negative**

- The verification email body is a plain-text template baked into the registration plugin; a
  pluggable templating hook is on the roadmap.
- The resend rate limit is approximate: `retryAfterSeconds` is the rolling-hour ceiling, not
  the precise number of seconds until the oldest token in the window ages out. Tracking the
  oldest timestamp per user would tighten this; it is a deliberate simplification given the
  spec asked only for a per-hour ceiling.
- An OIDC-only application (no `qavo-auth-registration`) cannot use the login guard — the
  property is silently `false` if the property source is absent, which is the intended behavior.

## Alternatives considered

- **JWT-encoded verification token.** Rejected: requires either signing-key sharing across
  hosts or a self-encryption scheme; the table is trivial in comparison and gives single-use
  enforcement without revocation lists.
- **Separate `qavo_email_verification_resend_log` table for rate-limit precision.** Rejected
  for now in favor of the simpler `COUNT(...) WHERE created_at >= now - 1h` query on the
  existing rows. Revisit if precision becomes a requirement.
- **Block login on a non-existent user when the guard is on.** Rejected: would leak
  account-existence. The guard runs strictly after credential success.
