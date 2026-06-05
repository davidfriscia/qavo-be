# ADR 0007 — Temporary account lockout after repeated failed logins

**Status:** Accepted

## Context

Without throttling, a local-credential endpoint is an open invitation to credential-stuffing and
password-spray attacks: an attacker can submit unbounded guesses against a discovered username,
and there is no signal — either to the operator or to the legitimate user — that an account is
under attack. The platform's "secure by default" requirement (architecture §5.5) makes shipping
the login plugin without a brute-force defense a regression risk for every consuming application.

## Decision

Introduce a **time-bounded automatic lockout** in `qavo-security`, applied to the local
authentication strategy whenever the login plugin is in use.

- **Counting.** A per-user `failed_login_attempts` counter on `qavo_users` (Flyway `V0002`) is
  incremented on every authentication failure event emitted by Spring Security —
  `AbstractAuthenticationFailureEvent`, not only "bad password", to deny attackers a cheap
  username-enumeration vector. The counter resets on `AuthenticationSuccessEvent`.
- **Locking.** When the counter reaches `qavo.security.local.lockout.max-attempts` (default 5),
  `locked_until` is set to "now + `duration`" (default 15 minutes). Further failures within the
  lock window do **not** extend it — the window is the deterrent, and re-extension would let an
  attacker keep a victim locked out indefinitely by spraying after the threshold.
- **Surfacing.** Once locked, the login endpoint returns HTTP `423 Locked` carrying RFC 9457
  Problem Details with `code = "account-locked"` and an `unlocksAt` extension property holding
  the ISO-8601 unlock instant, so a client can render an accurate countdown without scraping
  human-readable text.
- **Self-clearing.** The `QavoUserDetailsService` treats `locked_until <= now` as "not locked".
  A user whose window has elapsed authenticates without administrator intervention; the success
  event then clears the column.
- **Clock injection.** A `Clock` bean (default `Clock.systemUTC()`) is shared by the lockout
  service and the JWT issuer, so tests substitute a fixed/mutable clock to drive deterministic
  lock-expiry assertions without `Thread.sleep`.
- **Configuration.** Three properties under `qavo.security.local.lockout.*` (enabled,
  max-attempts, duration). Setting `enabled = false` returns the platform to its pre-0.0.1
  behavior without removing the schema, so the feature can be disabled per environment without
  a migration rollback.

## Consequences

**Positive**

- The login endpoint is no longer a silent oracle for credential attacks; bursts trip a visible
  ceiling and operators can alert on the warning log.
- Clients can present a precise unlock time; the contract is machine-readable, not a translation
  problem.
- The window is self-clearing, so the support cost of "unlock my account" tickets is bounded.
- Treating every failure equally closes a username-enumeration side channel.

**Negative**

- Locked-out users cannot self-recover within the window; tuning `duration` too high hurts UX,
  too low weakens the deterrent.
- A determined attacker who knows a target username can deliberately lock it as a denial-of-
  service. This is a known trade-off in account-lockout policy (NIST SP 800-63B §5.2.2); future
  enhancements (IP-based throttling, CAPTCHA on retry) belong in a separate ADR.
- The `locked_until` column adds one indexed write per failure burst; negligible in practice but
  noted.

## Alternatives considered

- **No lockout, rely on application-layer rate limiting only.** Rejected: an application-level
  rate limit applies to the *caller* (typically an IP); a credential-stuffing botnet spreads
  guesses across many IPs, defeating it. Per-account lockout addresses the orthogonal axis.
- **Permanent lock after threshold, requiring admin reset.** Rejected: turns every accidental
  triple-fail into a support ticket and creates an obvious DoS vector against named users.
- **Exponential backoff per attempt instead of a hard window.** Rejected for now: more complex
  for both implementation and client UX, with little additional security benefit at the chosen
  thresholds. May be revisited in a successor ADR.
- **Throw `AccountLockedException` from the `UserDetailsService`.** Rejected: bypasses Spring
  Security's standard failure pipeline (so success/failure event listeners don't fire), and
  forces every authentication mechanism — not just DAO — to know about lockout. The current
  design lets Spring throw `LockedException` naturally, then translates it once at the
  controller boundary.
