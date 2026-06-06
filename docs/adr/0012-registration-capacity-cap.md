# ADR 0012 — Registration capacity cap

**Status:** Accepted (2026-Q1)

## Context

Operators of the registration plugin asked for a way to gate self-service sign-up by raw
volume: a closed beta with N seats, a free-tier daily ceiling, or a launch-day capacity guard
that protects downstream systems while marketing traffic spikes. We needed an answer that:

- **Defaults to off** so existing deployments see no behavioral change.
- **Surfaces a precise, machine-readable response** the frontend can render without ad-hoc
  parsing.
- **Costs nothing when disabled** — no extra DB round-trips, no extra beans wired beyond a
  no-op placeholder.
- **Is the *platform's* job**, not every application's — the contract lives once in
  `qavo-core` as an SPI so future strategies (in-memory, Redis-backed, distributed-lock) can
  swap in without rewriting the registration flow.

## Decision

### Trigger surface — `503` not `429`

A reached cap returns **`503 Service Unavailable`** (with `Retry-After`) rather than
`429 Too Many Requests`.

- `429` is a *per-client rate-limit* signal: it tells the requesting client to slow down.
- This cap is a *global capacity* signal: even a brand-new client that has never made a
  request is rejected. Semantically that is service unavailability, not client misbehavior.

The frontend therefore handles cap rejections in the same code path as planned-maintenance
windows (poll the public status endpoint, render a wait UI), not in the per-form rate-limit
path.

The body is RFC 9457 Problem Details with two extension members:

| Member | Meaning |
|---|---|
| `opensAt` | ISO-8601 UTC instant when the next slot is expected to free up. |
| `retryAfter` | Whole, non-negative seconds between the check and `opensAt`. |

The HTTP `Retry-After` header carries the same number of seconds so off-the-shelf clients
(curl `--retry`, load balancers) honor it without inspecting the body.

### Window — rolling, not fixed

A rolling window (`now - configuredDuration`, re-evaluated on every check) is used instead of
a fixed reset boundary (midnight UTC, top of the hour). Three reasons:

1. **No thundering herd.** A fixed reset funnels every client that was rejected at 23:59 into
   the same 00:00 retry; a rolling window means slots free up continuously as old events age
   off, so the retry pressure is smoothed.
2. **No "free re-enable" race.** With a fixed reset an operator who lowers `max-registrations`
   mid-window cannot re-tighten until the next reset; with a rolling window the new limit
   applies immediately on the next check.
3. **The cost of an exact count is the same.** Both approaches are `SELECT count(*) WHERE
   registered_at > ?` — the rolling window simply substitutes `now - window` for `last reset`.

The implementation does not pre-compute or cache `opensAt`; it derives it on the fly from the
oldest event in the current window so a single ages-off step is reflected immediately.

### Storage — DB-backed, not in-memory

Counts are persisted in `qavo_registration_events` (`id`, `user_id`, `registered_at`).

- **Restart-safe.** A pod restart at 49 / 50 registrations does not reset the counter to zero.
- **Multi-instance friendly.** Multiple platform instances pointed at the same database share
  the count without any coordination protocol.
- **Naturally observable.** The same table is the audit ledger; ops can `SELECT … GROUP BY
  date_trunc('hour', registered_at)` to investigate after the fact.

In-memory counters were rejected because they would have to be either per-instance
(meaningless multi-tenant: a 50-cap deployed twice accepts 100) or backed by an out-of-process
store (Redis), which introduces an external dependency the platform deliberately does not
have. A Redis-backed strategy can still be added later as a second `RegistrationCapService`
implementation.

### Concurrency — soft cap, no distributed lock

The check-then-record sequence is **not** protected by `SELECT … FOR UPDATE` or a database
advisory lock. Under concurrent load the configured maximum can transiently be exceeded by a
small margin (at most one extra registration per concurrent in-flight check).

This is the deliberate trade-off:

- A distributed lock serializes every registration through a single DB row, capping platform
  throughput at one registration at a time — unacceptable for a feature that is supposed to
  *protect* the platform, not become its bottleneck.
- The cap is a capacity guard, not a regulatory quota. A 50-user beta that briefly ends up at
  53 is operationally indistinguishable from one that ends at 50.
- The `qavo.registration.cap.utilization` gauge makes any sustained over-cap visible to ops,
  who can lower `max-registrations` if a strict ceiling is needed.

`recordRegistration` runs with `Propagation.REQUIRES_NEW` so the audit row commits
independently of the outer registration transaction. If the outer flow rolls back after the
event row is written, the cap counts a "successful as far as user creation" attempt — which
is the conservative choice (slightly *over*-counting protects capacity).

### Status endpoint — always `200 OK`

`GET /api/v1/auth/registration-status` is public, returns `Cache-Control: no-store`, and
always responds **`200 OK`** — even when the cap is reached.

- Polling a `503` would mean frontends light up generic "service unavailable" banners on a
  perfectly healthy backend. The status endpoint is intentionally a state query, not a guarded
  resource.
- The response is the same DTO as the body of a rejected `/register` 503, minus the RFC 9457
  envelope: `open`, `currentCount`, `maxRegistrations`, `windowDuration`, `opensAt`,
  `retryAfter`, `checkedAt`. When `open=true` the count/limit/opensAt fields are omitted
  (`@JsonInclude(NON_NULL)`).

### `include-unverified` — opt-in expense

`qavo.auth.registration.cap.include-unverified` defaults to **`true`**: every registration
event counts. When set to `false`, only events whose user is currently `email_verified=true`
count.

- The verified-only mode is more permissive — it lets a "zombie" sign-up that never confirmed
  its email roll off the cap immediately — but it costs one user-table lookup per event in
  the window.
- The default is `true` because (a) most deployments care about capacity end-to-end, not
  conversion, and (b) it works with zero email-verification configuration. The verified-only
  mode is documented as "enable only if you also enable the verification flow", because
  without verification nothing ever flips the flag and the cap is effectively always open.
- The implementation avoids a cross-entity JPQL join (which would require dialect-specific
  `cast(... as String)` between `user_id VARCHAR` and `QavoUser.id UUID`); instead it streams
  user-ids in the window and fans out to `QavoUserRepository::findById`. List size is bounded
  by `max-registrations`, so the cost is predictable.

### Configuration shape & fail-fast validation

```yaml
qavo:
  auth:
    registration:
      cap:
        enabled: true
        max-registrations: 50
        window: PT24H
        include-unverified: true
```

`QavoRegistrationCapProperties` runs a `@PostConstruct` validator that throws
`BeanCreationException` when `enabled=true` with a missing/non-positive `max-registrations` or
`window`. We chose application-start failure over runtime no-ops so misconfigured deployments
break in CI / on first boot rather than silently letting every registration through.

### Observability

Three Micrometer meters, registered in both the database and the no-op service so the names
are stable across toggles:

| Meter | Type | Tags | Meaning |
|---|---|---|---|
| `qavo.registration.cap.check` | Counter | `result=allowed\|rejected` | One increment per cap-check call. |
| `qavo.registration.cap.current_count` | Gauge | — | Count inside the active rolling window. |
| `qavo.registration.cap.utilization` | Gauge | — | `currentCount / max` in `[0.0, 1.0]`. |

Micrometer is an **optional** dependency of the registration plugin; when absent, an
autoconfigured `SimpleMeterRegistry` fallback keeps the cap services functional without
shipping metrics anywhere.

### Schema

`V0011__qavo_registration_events.sql` (registration plugin's V0010+ band; V0020 is taken by
the login plugin's refresh tokens):

```sql
CREATE TABLE qavo_registration_events (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       VARCHAR(255) NOT NULL,
    registered_at TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_registration_events_registered_at
    ON qavo_registration_events (registered_at);
```

Plain `TIMESTAMP` (not `TIMESTAMP WITH TIME ZONE`) is used to stay compatible with the H2
mode used by the reference application's tests. `user_id` is `VARCHAR(255)` rather than
`UUID` so a future strategy that wants to record a tenant prefix or pseudonymous identifier
can do so without a migration.

## Consequences

**Positive**

- Operators get a true capacity guard with zero behavioral change for unaffected deployments.
- The frontend has a stable contract (status DTO + RFC 9457 + `Retry-After`) and is not
  responsible for the count itself.
- The SPI (`RegistrationCapService` in `qavo-core`) leaves room for in-memory or Redis-backed
  strategies without changing call sites.

**Negative**

- The cap is *soft*: under concurrent load the maximum can transiently be exceeded. Operators
  who need a strict quota must layer a domain-specific check on top.
- The verified-only mode adds one user lookup per event in the window — fine for caps in the
  hundreds, expensive at thousands. Documented in the configuration reference.
- The status endpoint is poll-only; there is no server-push channel. Frontends are expected
  to back off using `opensAt` / `retryAfter` rather than hammer the endpoint.

## Alternatives considered

- **HTTP 429.** Rejected: semantics mismatch (see "Trigger surface").
- **Fixed reset window (e.g. midnight UTC).** Rejected: thundering-herd on reset; no immediate
  effect of mid-window operator changes.
- **In-memory counter on each instance.** Rejected: not restart-safe; ambiguous in multi-instance
  deployments.
- **`SELECT … FOR UPDATE` lock around check+record.** Rejected: serializes registration
  throughput; provides a strictness the contract does not advertise.
- **Per-tenant cap.** Out of scope for 0.0.3; the SPI accepts a `userId` string on
  `recordRegistration` so a future tenant-aware implementation can partition counts by parsing
  it.
