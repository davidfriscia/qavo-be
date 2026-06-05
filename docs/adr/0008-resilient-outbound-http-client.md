# ADR 0008 — Resilient outbound HTTP client

**Status:** Accepted

## Context

Every non-trivial Qavo-based service eventually calls another HTTP service: an identity provider,
an internal microservice, a third-party API. Left to its own devices, each team reinvents the
same brittle plumbing — a raw `RestTemplate` or `RestClient` with no timeout, no retry, no
circuit breaker, no trace-header propagation — and discovers the gaps only when an upstream
slow-down cascades into thread-pool exhaustion or distributed traces dead-end at the network
boundary. The "secure and resilient by default" mandate in architecture §5.7 requires the
platform to ship one standardized client that handles these cross-cutting concerns out of the
box.

## Decision

Introduce a new `qavo-resilience` module exposing **`QavoHttpClient`**, a thin façade over
Spring's synchronous `RestClient` instrumented with Resilience4j and Qavo's `TraceContext`.

- **Sync over reactive.** `RestClient` (not `WebClient`) is the default. The platform's request
  model is overwhelmingly thread-per-request; a `Mono`/`Flux` boundary would force callers to
  either block (defeating the point) or reactify their entire stack (a much larger commitment
  than this ADR's scope). Reactive callers can still use `WebClient` directly when they need it.
- **Functional decoration.** Resilience4j is applied via direct `Retry.decorateSupplier` +
  `circuitBreaker.executeSupplier` composition rather than the `@CircuitBreaker` /
  `@Retry` AOP annotations. Annotations require proxying — which silently breaks on
  self-invocation and obscures the call-graph during incident review — and the functional form
  makes the ordering (retry innermost, breaker outermost) explicit in the source.
- **Per-client policies, looked up by name.** Each entry in
  `qavo.resilience.http.clients.<name>` produces one `QavoHttpClient` whose breaker and retry
  instances are looked up from the standard Resilience4j registries under the same `<name>`. This
  keeps operator-facing tuning where the Resilience4j docs already say it lives
  (`resilience4j.circuitbreaker.instances.<name>.*`) instead of inventing a parallel namespace.
- **Trace propagation as an interceptor.** A single `ClientHttpRequestInterceptor` reads
  `TraceContext.currentTraceId()` and copies it onto every outbound request under the configured
  header (default `X-Trace-Id`, override via `qavo.resilience.http.trace-header`). Putting this
  in an interceptor — rather than relying on Micrometer Observation propagation — means the same
  header works regardless of whether the consumer has wired Micrometer Tracing or is reading the
  trace ID from MDC populated by the request-id filter.
- **Registry, not bean-per-client.** Clients are exposed through a `QavoHttpClientRegistry`
  rather than as individually named beans, so adding a backend in YAML never requires also
  adding a `@Bean` definition or a corresponding `@Qualifier`.
- **WireMock as the standardized test stub.** The reactor pins `org.wiremock:wiremock-standalone`
  in the BOM for the explicit purpose of resilience testing; the standalone artifact embeds
  Jetty so it cannot collide with the application's servlet container at runtime.

## Consequences

- Services declare `qavo-resilience` once and configure backends declaratively — no per-service
  retry/breaker boilerplate.
- A single uniform trace header crosses every service boundary, so distributed traces stitch
  end-to-end without per-call instrumentation.
- Switching to a reactive client later is a non-breaking change for callers: the
  `QavoHttpClient` interface is transport-agnostic and a future `WebClient`-backed
  implementation can be registered side-by-side.
- Teams that need fine-grained Resilience4j features beyond retry + breaker (bulkhead, rate
  limiter, time limiter) can still wire them directly against the standard registries; the
  facade is a sensible default, not a cap.
