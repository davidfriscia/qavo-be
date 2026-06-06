# Configuring notifications in a Qavo application

This tutorial walks through enabling the two built-in notification channels shipped by
[`qavo-notifications`](../qavo-notifications): **email** (using
[Lettermint](https://lettermint.co) as a sample SMTP provider) and **Telegram**. It targets
application developers who already have a Qavo-based service running and now want delivery to
external channels.

For the design and contract behind the abstraction, see
[ADR 0010 — Pluggable notifications abstraction](adr/0010-notifications-abstraction.md).
For the property surface, see the
[Notifications section of the configuration reference](qavo-configuration-reference.md).

---

## 1. Mental model

The platform exposes a single facade:

```java
NotificationResult result = dispatcher.dispatch(
        NotificationRequest.email(recipient, subject, body));
```

Calling code never imports JavaMail or any Telegram client. The `qavo-notifications` module
auto-registers one `NotificationService` bean per enabled channel; the dispatcher routes the
request to the first provider whose `supports(channel)` returns true. **Failures never throw**
— a missing SMTP server, an expired Telegram token, or a network outage all surface as
`NotificationResult.failure(...)` and are logged at WARN. Business operations are not blocked
by transport health.

The general adoption flow is:

1. **Provider side** — create an account, obtain delivery credentials, validate sender
   identity (domain DNS for email, bot ownership for Telegram).
2. **Application side** — add the `qavo-notifications` dependency, point Spring Boot's
   transport configuration at the provider, and enable the matching `qavo.notifications.*`
   channel.
3. **Verify** — send a probe through the dispatcher and check `NotificationResult.success()`
   plus the `qavo.notifications.sent` Micrometer counter tagged with `status=success`.

---

## 2. Add the dependency

Once on every application that wants to send notifications:

```xml
<dependency>
  <groupId>org.qavo</groupId>
  <artifactId>qavo-notifications</artifactId>
</dependency>
```

The BOM (`qavo-bom`) pins the version, so do not specify one. If you also want Telegram, add
`qavo-resilience` (the Telegram provider uses `QavoHttpClient` for retries and trace
propagation):

```xml
<dependency>
  <groupId>org.qavo</groupId>
  <artifactId>qavo-resilience</artifactId>
</dependency>
```

For SMTP you additionally need Spring Boot's mail starter:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

---

## 3. Channel A — Email via Lettermint (SMTP)

Lettermint is a transactional email service; the same procedure applies to any standard SMTP
provider (Postmark, Mailgun, SendGrid, AWS SES via SMTP, …). Only the host, port, and
credentials differ.

### 3.1 Provider side — what to do on Lettermint

These steps happen in the Lettermint dashboard, **before** touching the application:

1. **Sign up and create a project.** A project groups senders, templates, and API keys.
2. **Verify the sending domain.** Lettermint asks you to publish DNS records (SPF, DKIM, and
   typically a Return-Path / DMARC alignment record) for the domain you intend to send from
   (e.g. `mail.example.com`). Without this, downstream mail servers will reject or spam-bin
   the messages — verification is mandatory, not optional.
3. **Create an SMTP credential.** In the project settings, generate a username/password pair
   (often called an "SMTP token" or "API user"). Treat the password like any other secret:
   put it in a secrets manager or environment variable, never in source control.
4. **Note the SMTP server details.** Lettermint provides them in the dashboard; the typical
   shape is:
   - host: `smtp.lettermint.co`
   - port: `587` (STARTTLS) or `465` (implicit TLS)
   - auth: SMTP AUTH with the credential created in step 3
5. **Choose your `From` address.** It must belong to the verified domain. Pick a stable
   no-reply mailbox like `no-reply@example.com`; the same address will be the default sender
   for every email Qavo dispatches.
6. **(Optional) Configure a webhook** for delivery/bounce events if you need to react to them
   in your application — Qavo does not consume these out of the box but you can implement a
   simple controller that processes them.

> Replace `smtp.lettermint.co` with the exact host Lettermint shows in your dashboard.
> Provider hostnames change occasionally; the dashboard is authoritative.

### 3.2 Application side — Spring Boot mail + Qavo properties

`application.yml`:

```yaml
spring:
  mail:
    host: smtp.lettermint.co
    port: 587
    username: ${LETTERMINT_SMTP_USERNAME}
    password: ${LETTERMINT_SMTP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
      mail.smtp.starttls.required: true
      mail.smtp.connectiontimeout: 5000
      mail.smtp.timeout: 5000
      mail.smtp.writetimeout: 5000

qavo:
  notifications:
    enabled: true
    email:
      enabled: true
      from: no-reply@example.com   # must belong to a verified Lettermint domain
```

What this configures:

- Spring Boot's `MailSenderAutoConfiguration` builds a `JavaMailSender` bean from
  `spring.mail.*` and authenticates against Lettermint via STARTTLS.
- `qavo.notifications.email.enabled=true` flips on the
  `JavaMailNotificationService` provider. It is autowired with the `JavaMailSender` and uses
  `qavo.notifications.email.from` as the default sender.
- The dispatcher then routes any `NotificationRequest` with `channel=EMAIL` through this
  provider.

### 3.3 Secrets

Never check credentials into git. Either:

- Export them as environment variables before the JVM starts:
  ```bash
  export LETTERMINT_SMTP_USERNAME=...
  export LETTERMINT_SMTP_PASSWORD=...
  ```
- Or supply them through your secrets manager (HashiCorp Vault, AWS Secrets Manager, Azure
  Key Vault, Kubernetes `Secret` mounted as env vars, etc.). Spring's `${...}` placeholders
  resolve uniformly across all of these.

---

## 4. Channel B — Telegram

Telegram delivery uses the Bot API directly. There is no SMTP and no DNS — the provider step
is mostly bot creation.

### 4.1 Provider side — what to do on Telegram

1. **Create a bot via @BotFather.** In Telegram, start a chat with
   [`@BotFather`](https://t.me/BotFather) and send `/newbot`. BotFather will ask for a
   display name and a unique username ending in `bot` (e.g. `acme_alerts_bot`).
2. **Capture the bot token.** BotFather returns an HTTP token in the shape
   `123456789:ABCdefGhIJKlmnoPQRstuVWXyz`. This is the credential the application uses on
   every `sendMessage` call — store it as a secret.
3. **Identify your `chat_id`.**
   - For **personal alerts**, message the bot once from your own account, then call
     `https://api.telegram.org/bot<TOKEN>/getUpdates` and read `result[0].message.chat.id`.
     The id is your personal `chat_id` (a positive integer).
   - For **group/channel delivery**, add the bot to the group or channel, send any message,
     and read the `chat.id` (group ids are negative, e.g. `-1001234567890`). For channels
     the bot must be an administrator with the "post messages" permission.
4. **(Optional) Restrict bot privacy.** By default BotFather disables group-message reading
   for the bot — leave this off unless you actually need it; it tightens the bot's scope.

### 4.2 Application side — Qavo + resilient HTTP client

The Telegram provider sends every message through a `QavoHttpClient` so it benefits from
retries, circuit breakers, timeouts, and `traceId` propagation. You declare that client under
`qavo.resilience.http.clients.<name>` and then reference the same `<name>` in
`qavo.notifications.telegram.client-name`.

`application.yml`:

```yaml
qavo:
  notifications:
    enabled: true
    telegram:
      enabled: true
      bot-token: ${TELEGRAM_BOT_TOKEN}
      client-name: telegram        # matches the resilience client below
  resilience:
    http:
      clients:
        telegram:
          base-url: https://api.telegram.org
          connect-timeout: PT2S
          read-timeout: PT10S

resilience4j:
  retry:
    instances:
      telegram:
        max-attempts: 3
        wait-duration: PT1S
  circuitbreaker:
    instances:
      telegram:
        sliding-window-size: 10
        failure-rate-threshold: 50
```

What this configures:

- `QavoHttpClientRegistry` exposes a client named `telegram` pointing at
  `https://api.telegram.org` with sane timeouts.
- Resilience4j attaches a retry policy and a circuit breaker to the same name.
- `qavo.notifications.telegram.enabled=true` activates the `TelegramNotificationService`
  provider. It POSTs to `/bot{token}/sendMessage` through the resilient client.

> **Security note.** The bot token appears in the URL of every Telegram request. Keep it in a
> secret store, rotate it through BotFather if it leaks, and avoid logging request URIs at
> levels that ship to third-party log aggregators.

---

## 5. Sending a notification from application code

Inject the dispatcher anywhere — controllers, services, listeners. The same call site works
for any channel:

```java
@Service
class OperationsAlerts {

    private static final Logger log = LoggerFactory.getLogger(OperationsAlerts.class);

    private final NotificationDispatcher dispatcher;

    OperationsAlerts(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    void notifyCustomer(String email, String body) {
        NotificationResult result = dispatcher.dispatch(
                NotificationRequest.email(email, "Your order is ready", body));
        if (!result.success()) {
            log.warn("Customer email failed for {}: {}", email, result.errorMessage());
        }
    }

    void pageOnCall(String chatId, String text) {
        NotificationResult result = dispatcher.dispatch(
                NotificationRequest.telegram(chatId, text));
        if (!result.success()) {
            log.warn("Telegram page failed for chat={}: {}", chatId, result.errorMessage());
        }
    }
}
```

The same wiring is used internally by `qavo-auth-registration` for the email verification
flow — see the
[Email verification section of the integration guide](integration-guide.md#6d-email-verification-qavo-auth-registration).

---

## 6. Verifying the setup

1. **Boot the application** with the configuration above. On startup the
   `JavaMailNotificationService` and/or `TelegramNotificationService` beans should be
   registered; missing credentials don't fail startup, they only cause `dispatch(...)` to
   return failure.
2. **Send a probe**, e.g. from a one-off CLI command, a `CommandLineRunner` guarded by a
   profile, or an integration test:
   ```java
   dispatcher.dispatch(NotificationRequest.email(
           "you@example.com", "Qavo probe", "If you read this, SMTP works."));
   ```
3. **Check the metric.** Micrometer exposes:
   - `qavo.notifications.sent{channel=EMAIL,status=success}`
   - `qavo.notifications.sent{channel=EMAIL,status=failure}`
   - `qavo.notifications.sent{channel=TELEGRAM,status=success}` / `=failure`

   Grafana or `actuator/metrics/qavo.notifications.sent` should show a non-zero
   `status=success` count.
4. **If a probe fails,** read the application log: the providers log the underlying SMTP /
   HTTP error at WARN with the recipient and a brief reason. For Lettermint, also check the
   provider dashboard's delivery log — it will show whether the message reached Lettermint at
   all or was rejected at submission.

---

## 7. Common pitfalls

| Symptom | Likely cause | Fix |
|---|---|---|
| Every email fails with "535 Authentication failed" | Wrong SMTP credential, or credential pasted with leading/trailing whitespace | Regenerate the credential in Lettermint, store it via env var, restart |
| Emails accepted by SMTP but never arrive | DNS records (SPF/DKIM) missing or mis-published | Re-check the DNS instructions in the Lettermint dashboard; verification status must be "verified" |
| `qavo.notifications.email.from must be configured` in logs | `qavo.notifications.email.from` is blank | Set it to a mailbox on the verified domain |
| `qavo.notifications.telegram.bot-token must be configured` | Property is empty or the env var didn't resolve | Confirm the placeholder, e.g. `bot-token: ${TELEGRAM_BOT_TOKEN}` |
| Telegram returns `401 Unauthorized` | Token revoked or copied incorrectly | Issue a new token via BotFather (`/token`) and rotate the secret |
| Telegram returns `400 Bad Request: chat not found` | Wrong `chat_id`, or the bot was never added to the group / made a channel admin | Re-derive the `chat_id` via `getUpdates`; for channels make the bot an admin with post permission |
| No provider runs at all, dispatcher always logs "no provider supports channel" | The notifications module isn't on the classpath, or `qavo.notifications.enabled=false` | Add the dependency; ensure no profile overrides the master switch |

---

## 8. Where to go next

- [ADR 0010](adr/0010-notifications-abstraction.md) — design rationale and the SPI contract.
- [Best practices §3a](best-practices.md#3a-notification-dispatch-patterns) — patterns for
  call-site error handling and adding custom channels.
- [Configuration reference](qavo-configuration-reference.md) — exhaustive property table.
- [ADR 0011](adr/0011-email-verification-design.md) — the email verification flow that
  consumes this module end-to-end; a complete worked example of the dispatcher in production
  use.
