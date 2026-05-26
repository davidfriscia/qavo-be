# ADR 0003 — RFC 9457 Problem Details as the error contract

**Status:** Accepted

## Context

Every Qavo-based application must return errors in the same, machine-readable format, handled in one
place, and correlated to logs (architecture §5.2, §5.3).

## Decision

Adopt **RFC 9457 (Problem Details for HTTP APIs)** as the single error contract. The core provides:

- `ProblemDetailFactory` — builds Spring's `ProblemDetail` with the platform's standard extensions:
  `code`, `timestamp`, `traceId`, and (for validation) an `errors` array.
- `ProblemType` / `CoreProblemType` — stable error classifications mapping to `type` URIs, titles,
  and HTTP statuses; applications contribute their own `ProblemType`s.
- A base exception hierarchy (`QavoException` and subclasses) carrying a `ProblemType`.
- A global `@RestControllerAdvice` in the web starter that translates every exception — platform,
  framework, and validation — into this shape.

The `traceId` extension ties each error response to its structured log entries.

## Consequences

**Positive**
- Consistent, standard, machine-readable errors across all applications and plugins.
- Centralized handling; controllers never format errors.
- End-to-end traceability via `traceId`.

**Negative**
- Applications must throw platform exceptions (or `ProblemType`s) rather than ad-hoc ones to benefit.
- The `type` URIs reference a base (`qavo.error.base-uri`) that should eventually host human-readable
  documentation per type.

## Alternatives considered

- **Bespoke error envelope.** Rejected: reinvents a solved problem and is non-standard for clients.
- **Spring's default `ProblemDetail` without extensions.** Rejected: lacks `traceId`/`timestamp`/
  `code`/structured `errors`, which the platform guarantees.
