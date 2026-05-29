# Qavo Backend (`qavo-be`)

> **Qavo — the foundation your applications stand on.**

`qavo-be` is the **backend** half of the Qavo platform: a reusable, modular Spring Boot
foundation that centralizes the cross-cutting concerns every application needs — security, error
handling, logging, validation, observability, API documentation, and a plugin system — and
distributes them as independently versioned Maven artifacts.

It is **not a business application**. It is a platform/framework that many applications depend on,
import a few modules from, follow a handful of conventions, and inherit consistent, secure-by-default
behavior. A fix or improvement propagates to all consumers through a version bump.

- **Master project:** https://github.com/davidfriscia/qavo
- **This repository (backend):** `qavo-be`
- **Frontend counterpart:** `qavo-fe` (Angular; not part of this repository)
- **Current version:** `0.0.0-SNAPSHOT` (early foundation stage — see [Maturity](#maturity-disclaimer))

---

## Table of contents

- [Architectural purpose](#architectural-purpose)
- [Technology stack](#technology-stack)
- [Module overview](#module-overview)
- [Architecture summary](#architecture-summary)
- [Quick start](#quick-start)
- [Build instructions](#build-instructions)
- [Local development](#local-development)
- [Publishing overview](#publishing-overview)
- [Integration examples](#integration-examples)
- [Documentation](#documentation)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Maturity disclaimer](#maturity-disclaimer)
- [License](#license)

---

## Architectural purpose

The platform is built on a clear separation between **Platform** (Qavo) and **Applications**:

- **Qavo** is a set of versioned libraries containing all the shared, cross-cutting behavior.
- **Applications** declare a dependency on a specific Qavo version, import the core starter plus
  only the capability plugins they need, and focus on their own domain.

Eight principles drive the design (see the [reference architecture](architecture.md)):
centralization of cross-cutting concerns, independent evolution, modularity by composition,
convention over configuration, clean layer separation, mobile-first/responsive (frontend),
secure by default, and long-term maintainability.

## Technology stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 21 (LTS) |
| Framework | Spring Boot 3.3.x |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA + Hibernate |
| Migrations | Flyway (modular, per-module locations) |
| Security | Spring Security (local DB baseline + OAuth2/OIDC resource server) |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| Observability | Micrometer + OpenTelemetry bridge + structured JSON logging (Logback + logstash encoder) |
| API docs | springdoc-openapi (OpenAPI 3 + Swagger UI) |
| Build | Maven (multi-module, BOM-driven) |
| Testing | JUnit 5, Spring Test, Testcontainers |

## Module overview

```text
qavo-be/
├── qavo-bom              # Bill of Materials: pins all module + third-party versions
├── qavo-core             # Stable contracts: error model, exceptions, pagination,
│                         #   security-context abstraction, plugin SPI, feature flags,
│                         #   modular Flyway aggregation, configuration properties
├── qavo-observability    # Structured JSON logging, MDC trace propagation, Actuator/Micrometer
├── qavo-validation       # Bean Validation integration, custom constraints, error mapping
├── qavo-security         # Pluggable auth (local DB baseline + OIDC), secure headers, CORS
├── qavo-openapi          # springdoc-openapi integration, plugin-aware documentation
├── qavo-starter-web      # The "always import" starter: aggregates the above + web wiring
├── qavo-auth-login       # Plugin: local login flow (/api/v1/auth/login)
├── qavo-auth-registration# Plugin: self-service registration (/api/v1/auth/register)
├── qavo-test-support     # Reusable test infrastructure (Testcontainers, assertions)
└── qavo-reference-app    # Runnable example application composing core + plugins
```

See [`docs/capabilities-matrix.md`](docs/capabilities-matrix.md) for what each concern implements
today and what is planned.

## Architecture summary

Applications follow a clean four-layer structure; Qavo provides the cross-cutting foundation:

```text
PRESENTATION  (api)            REST controllers, DTOs — no business logic
APPLICATION   (application)    Services, use-case orchestration, transactions
DOMAIN        (domain)         Entities, value objects, invariants — framework-free
INFRASTRUCTURE(infrastructure) Repositories, external clients
        ▲ cross-cutting (provided by Qavo): security · errors · logging · validation · ...
```

Capabilities beyond the core are delivered as **plugins**: separate modules that auto-configure
themselves, contribute endpoints/services/migrations, and can be removed by dropping the
dependency. See [ADR-0001](docs/adr/0001-modular-monolith-and-plugins.md).

## Quick start

**Prerequisites:** JDK 21 and Maven 3.9+ (or generate the Maven Wrapper, see below). Docker is
required only for Testcontainers-based integration tests.

```bash
# Build everything and run unit/integration tests
mvn clean verify

# Run the reference application (zero-setup, in-memory H2)
mvn -pl qavo-reference-app spring-boot:run
```

Then explore:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI document: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`
- Register a user: `POST http://localhost:8080/api/v1/auth/register`

## Build instructions

```bash
mvn clean install          # build + test + install all modules to the local repo
mvn -pl qavo-core test     # test a single module
mvn -pl qavo-reference-app -am package   # build the reference app and its dependencies
```

> **Maven Wrapper:** this repository ships `.mvn/wrapper/maven-wrapper.properties`. To generate the
> wrapper scripts/binary from a local Maven, run `mvn -N wrapper:wrapper -Dmaven=3.9.9`, after which
> `./mvnw` / `mvnw.cmd` are available.

## Local development

- **Run the reference app against PostgreSQL** instead of H2 by setting the standard Spring
  datasource environment variables and activating the `prod` profile for JSON logging:

  ```bash
  export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/qavo
  export SPRING_DATASOURCE_USERNAME=qavo
  export SPRING_DATASOURCE_PASSWORD=secret
  mvn -pl qavo-reference-app spring-boot:run -Dspring-boot.run.profiles=prod
  ```

- **Configuration namespaces:** platform settings live under `qavo.*`, application settings under
  `app.*`. Secrets are supplied via environment variables, never committed. See
  [`docs/qavo-configuration-reference.md`](docs/qavo-configuration-reference.md).

## Publishing overview

Qavo follows [Semantic Versioning](https://semver.org/). On release, modules are published as
signed artifacts to **Maven Central** under the `org.qavo` group, governed by `qavo-bom`. The core
moves slowly and carefully; plugins evolve faster and independently. Releases are tag-triggered from
CI. (The publishing pipeline configuration is part of the roadmap at this snapshot stage.)

## Integration examples

A consuming application imports the BOM, the core starter, and the plugins it wants:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.qavo</groupId>
      <artifactId>qavo-bom</artifactId>
      <version>0.0.0-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>org.qavo</groupId>
    <artifactId>qavo-starter-web</artifactId>   <!-- always -->
  </dependency>
  <dependency>
    <groupId>org.qavo</groupId>
    <artifactId>qavo-auth-login</artifactId>     <!-- optional plugin -->
  </dependency>
</dependencies>
```

Full walkthrough: [`docs/integration-guide.md`](docs/integration-guide.md).

## Documentation

| Document | Purpose |
|---|---|
| [`architecture.md`](architecture.md) | Authoritative reference architecture (v1.3) |
| [`docs/integration-guide.md`](docs/integration-guide.md) | How applications adopt and extend Qavo |
| [`docs/capabilities-matrix.md`](docs/capabilities-matrix.md) | Concern-by-concern implementation status |
| [`docs/roadmap.md`](docs/roadmap.md) | Prioritized TODO roadmap (implemented / partial / planned) |
| [`docs/best-practices.md`](docs/best-practices.md) | Patterns, plugin/extension guides, anti-patterns |
| [`docs/qavo-configuration-reference.md`](docs/qavo-configuration-reference.md) | Every `qavo.*` property |
| [`docs/adr/`](docs/adr/) | Architecture Decision Records |

## Roadmap

See [`docs/roadmap.md`](docs/roadmap.md). Headlines: signed-token issuance for local login,
auditing via Spring Data, resilience4j HTTP client wiring, i18n message bundles, CI/CD publishing,
and a richer OIDC claim-mapping story.

## Contributing

Contributions are welcome. Please read [`CONTRIBUTING.md`](CONTRIBUTING.md),
[`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md), and the responsible-disclosure policy in
[`SECURITY.md`](SECURITY.md). Qavo is developed with AI assistance under human review and
accountability (architecture §12); the same quality and security bar applies to all contributions.

## Maturity disclaimer

This is version **`0.0.0-SNAPSHOT`**: an early foundation. The **architecture, contracts, and
extension points are in place and consistent**, but several capabilities are intentionally partial
(clearly marked in the [capabilities matrix](docs/capabilities-matrix.md) and [roadmap](docs/roadmap.md)).
APIs may change before `1.0.0`. It is suitable for evaluation and as a starting point, not yet for
production.

## License

Licensed under the [MIT License](LICENSE).
