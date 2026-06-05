# Qavo Backend — Integration Guide

This guide explains how an application adopts the Qavo backend platform, configures it, installs
plugins, and extends it. It complements the authoritative [reference architecture](../architecture.md)
(§7 in particular).

> **Target experience:** declare the Qavo version, import the core starter plus the plugins you
> need, follow a few conventions, and the common behavior is already there.

---

## 1. Add the BOM and the modules

Import `qavo-bom` once in `dependencyManagement`; it pins the versions of every Qavo module and the
third-party libraries the platform standardizes on. Then declare modules **without versions**.

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.qavo</groupId>
      <artifactId>qavo-bom</artifactId>
      <version>0.0.1-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <!-- The "always import" core starter: security, errors, logging, validation, OpenAPI. -->
  <dependency>
    <groupId>org.qavo</groupId>
    <artifactId>qavo-starter-web</artifactId>
  </dependency>

  <!-- Optional plugins — only what this application needs. -->
  <dependency>
    <groupId>org.qavo</groupId>
    <artifactId>qavo-auth-login</artifactId>
  </dependency>
  <dependency>
    <groupId>org.qavo</groupId>
    <artifactId>qavo-auth-registration</artifactId>
  </dependency>

  <!-- Optional cross-cutting modules — add when needed. -->
  <dependency>
    <groupId>org.qavo</groupId>
    <artifactId>qavo-resilience</artifactId>   <!-- resilient outbound HTTP client -->
  </dependency>
  <dependency>
    <groupId>org.qavo</groupId>
    <artifactId>qavo-auditing</artifactId>     <!-- JPA created/modified auditing -->
  </dependency>

  <!-- Persistence is the application's choice; qavo-security treats JPA as optional. -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>
  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
  </dependency>
  <!-- ... a JDBC driver (e.g. org.postgresql:postgresql) ... -->
</dependencies>
```

## 2. Configure application-specific values

Auto-configuration applies sensible, secure defaults. In `application.yml` you provide only what is
specific to your application. Platform settings live under `qavo.*`; your own under `app.*`.

```yaml
qavo:
  api:
    title: My Application API
    version: v1
  observability:
    application-name: my-app
  security:
    strategy: local            # local | oidc | hybrid
    # For OIDC / hybrid:
    # oidc:
    #   issuer-uri: https://login.microsoftonline.com/<tenant>/v2.0
    #   authorities-claim: roles
  auth:
    registration:
      self-service: true
      require-email-verification: false

app:
  # your application configuration here
```

The full list of properties is in [`qavo-configuration-reference.md`](qavo-configuration-reference.md).

## 3. Develop the domain

Follow the package convention (architecture §4) inside your application:

```text
com.example.myapp
├── api             # @RestController, request/response DTOs (no business logic)
├── application     # @Service use cases, transactions
├── domain          # entities, value objects, invariants
└── infrastructure  # repositories, external clients
```

Build controllers under the inherited `/api/v1` prefix (use `ApiConventions.BASE_PATH`), return the
standard `PagedResponse<T>` envelope for collections, and throw the platform exceptions
(`ResourceNotFoundException`, `ConflictException`, `BusinessException`, …) — the global handler
renders them as RFC 9457 Problem Details automatically.

```java
@RestController
@RequestMapping(ApiConventions.BASE_PATH + "/widgets")
class WidgetController {

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    PagedResponse<WidgetResponse> list(Pageable pageable) {
        return PagedResponse.from(service.list(pageable), WidgetResponse::from);
    }
}
```

## 4. Security setup

The platform is **secure by default**: strict HTTP headers, stateless sessions, same-origin CORS,
and "authenticated by default" authorization are active with no code.

- **Public endpoints:** list Ant patterns under `qavo.security.public-paths`, or contribute a
  `PublicPathContributor` bean from a module.
- **Local authentication:** the default. Users/roles/permissions live in the platform's tables
  (created by Flyway). Inject `SecurityContextAccessor` to read the current principal — never touch
  `SecurityContextHolder` directly.
- **OIDC / OAuth2:** set `qavo.security.strategy: oidc` (or `hybrid`), add
  `spring-boot-starter-oauth2-resource-server`, and provide `qavo.security.oidc.issuer-uri`. Token
  validation and claim→authority mapping are handled for you.
- **CORS:** enable and declare origins explicitly:

  ```yaml
  qavo:
    security:
      cors:
        enabled: true
        allowed-origins: [ "https://app.example.com" ]
  ```

## 5. Database migrations

Each module ships its own Flyway migrations; the platform merges all locations automatically.
**Version banding convention:** the platform reserves versions `V0001`–`V0099`. Applications start
their own migrations at `V0100` to avoid collisions in the shared Flyway history.

## 6. Installing and removing plugins

- **Install:** add the plugin dependency. It auto-configures itself (conditional on classpath +
  properties), contributes its routes, services and migrations, and registers itself in the plugin
  inventory (visible as the `x-qavo-plugins` OpenAPI extension and via the `PluginRegistry` bean).
- **Tune behavior:** once imported, configure under `qavo.<plugin>.*` (e.g.
  `qavo.auth.registration.self-service`).
- **Remove:** drop the dependency. Its code, endpoints and table ownership leave with it.

## 6a. Resilient outbound HTTP calls (`qavo-resilience`)

Declare each upstream backend under `qavo.resilience.http.clients.<name>`; the platform builds one
`QavoHttpClient` per entry, looks up the Resilience4j retry + circuit-breaker instances by the same
`<name>` from the standard registries, and propagates the current `traceId` on every request under
`qavo.resilience.http.trace-header` (default `X-Trace-Id`). See
[ADR 0008](adr/0008-resilient-outbound-http-client.md).

```yaml
qavo:
  resilience:
    http:
      clients:
        billing:
          base-url: https://billing.internal.example.com
          connect-timeout: PT2S
          read-timeout: PT10S
resilience4j:
  retry.instances.billing:
    max-attempts: 3
    wait-duration: PT1S
  circuitbreaker.instances.billing:
    sliding-window-size: 10
    failure-rate-threshold: 50
```

```java
@Service
class BillingClient {
    private final QavoHttpClient http;
    BillingClient(QavoHttpClientRegistry registry) {
        this.http = registry.get("billing");
    }
    Invoice fetch(String id) {
        return http.get("/invoices/" + id, Invoice.class).getBody();
    }
}
```

## 6b. JPA auditing (`qavo-auditing`)

Add the module, then extend `AuditableEntity` on any JPA entity that should carry
`created_at` / `last_modified_at` / `created_by` / `last_modified_by`. The platform's
`QavoAuditorAware` resolves the auditor through `SecurityContextAccessor`, so the same wiring
works under local, OIDC, or hybrid auth. Writes that happen outside an authenticated request
are recorded as `qavo.auditing.system-principal` (default `"system"`). See
[ADR 0009](adr/0009-platform-jpa-auditing.md).

```java
@Entity
@Table(name = "widgets")
class Widget extends AuditableEntity {
    @Id private UUID id;
    private String name;
}
```

## 7. Extension points

| Extension point | Purpose |
|---|---|
| `ProblemType` | Contribute domain-specific error types (new `type`/`title`/status) |
| `QavoException` subclasses | Domain exceptions auto-mapped to Problem Details |
| `HttpSecurityCustomizer` | Add to the shared `SecurityFilterChain` without replacing it |
| `PublicPathContributor` | Declare endpoints reachable without authentication |
| `QavoPlugin` / `PluginDescriptor` | Register a capability in the plugin inventory |
| `MigrationLocation` | Contribute a module's Flyway migration location |
| `FeatureFlagService` | Replace or extend feature-flag evaluation |
| `QavoHttpClient` / `QavoHttpClientRegistry` | Resilient outbound HTTP calls with traceId propagation |
| `AuditableEntity` / `AuditorAware<String>` | Standard JPA audit columns; override the auditor bean to use a non-string type |
| Custom Bean Validation constraints | Reuse `StrongPassword`, `Slug`, or add your own |

See [`best-practices.md`](best-practices.md) for a full plugin development walkthrough.

## 8. API client generation (frontend)

The backend exposes its OpenAPI document at `/v3/api-docs`. The frontend generates a typed client
from it (openapi-generator), making the contract the single source of truth (architecture §7.3).
Plugins contribute to the same document.
