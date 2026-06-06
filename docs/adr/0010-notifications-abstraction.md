# ADR 0010 — Pluggable notifications abstraction

**Status:** Accepted (2026-Q1)

## Context

The platform repeatedly needs to send out-of-band messages — a verification email after
registration, a 2FA prompt, an operational alert to a Telegram channel, etc. Until now each
caller had to know which provider was active and how to talk to it. That creates two problems:

1. **Tight coupling.** A use case that sends a verification email cannot be reused as-is in an
   application that delivers verification through Telegram.
2. **Hard failure modes.** If a controller's happy path catches `MessagingException`, an
   unreachable SMTP server takes down user registration even though the user record itself was
   created without issue.

We want a small, stable seam every plugin can depend on without dragging in JavaMail or the
Telegram Bot API as a hard requirement.

## Decision

Introduce `org.qavo.core.notifications` in `qavo-core` and a new `qavo-notifications` plugin
implementing it.

### SPI in `qavo-core`

```text
NotificationChannel    enum { EMAIL, TELEGRAM, NONE }
NotificationRequest    record (channel, recipient, subject?, body, htmlBody?, metadata)
NotificationResult     record (boolean success, String providerMessageId, String errorMessage)
NotificationService    SPI : send(request) + supports(channel)
NotificationDispatcher facade : dispatch(request) → NotificationResult, never throws
```

`NotificationResult` is the contract for failure handling: every provider returns a result,
never throws. Callers branch on `result.success()` and log on failure.

### Built-in providers in `qavo-notifications`

| Provider | Activation | Notes |
|---|---|---|
| `JavaMailNotificationService` | `qavo.notifications.email.enabled=true` + `JavaMailSender` on the classpath | Uses `MimeMessageHelper`; supports plain or HTML body |
| `TelegramNotificationService` | `qavo.notifications.telegram.enabled=true` + `QavoHttpClientRegistry` available | POSTs `chat_id` + `text` to `/bot{token}/sendMessage` through the named client |
| `NoOpNotificationService` | Always; lowest priority | Fallback so the dispatcher always has a provider to call |

The autoconfiguration is annotated `@AutoConfiguration(after = MailSenderAutoConfiguration.class)`
so `@ConditionalOnBean(JavaMailSender.class)` evaluates after Spring Boot has had a chance to
register the bean (this was a recurring footgun and is now codified).

### Dispatcher behavior

`DefaultNotificationDispatcher` iterates the autowired `List<NotificationService>` in `@Order`
and calls the first one whose `supports(channel)` returns true. It records two Micrometer
metrics:

- `qavo.notifications.sent` counter, tagged with `channel` and `status` (`success`/`failure`)
- `qavo.notifications.providers.registered` gauge

## Consequences

**Positive**

- Application code (registration, password-reset, alerts, ...) depends only on the
  `NotificationDispatcher` interface from `qavo-core` and is portable across providers.
- A failed dispatch never propagates as an exception; business operations stay decoupled from
  transport health.
- Adding a new channel (SMS, web push) is a single new `NotificationService` bean — no changes
  to the dispatcher or to callers.

**Negative**

- One more module to keep in sync; consumers wanting EMAIL or TELEGRAM must opt in.
- The Telegram provider currently relies on `qavo-resilience`. Consumers not already using it
  pay the cost of declaring a `QavoHttpClient` named `telegram` (or whatever
  `qavo.notifications.telegram.client-name` is set to).

## Alternatives considered

- **Direct use of `JavaMailSender` everywhere.** Rejected: leaks transport details into use
  cases and locks the platform to email.
- **Spring `ApplicationEventPublisher` events instead of a facade.** Rejected: the dispatcher
  is intentionally synchronous so callers can branch on `NotificationResult.success()` for
  logging/metrics. Asynchronous fan-out can still be wrapped on top.
- **A larger `qavo-notifications-spi` artifact.** Rejected: the SPI is a handful of types and
  fits naturally in `qavo-core` next to the other cross-cutting contracts.
