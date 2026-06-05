# Qavo Backend — Best Practices

Practical guidance for building on and extending the Qavo platform. These conventions keep
applications consistent with one another and the platform maintainable over time.

---

## 1. Architecture patterns

### Respect the layering (architecture §4)

```text
api → application → domain ← infrastructure
```

- **Presentation (`api`)**: controllers and DTOs only. No business logic, no repository access.
- **Application (`application`)**: `@Service` use cases, transaction boundaries, orchestration.
- **Domain (`domain`)**: entities, value objects, invariants. Framework-free where practical.
- **Infrastructure (`infrastructure`)**: repositories and external clients.

The dependency rule is one-directional: upper layers know lower ones, never the reverse. The domain
depends on nothing, so it stays pure and testable.

### Use the platform contracts, not framework internals

```java
// Good — strategy-independent, swappable
private final SecurityContextAccessor securityContext;
var user = securityContext.currentPrincipal().orElseThrow();

// Avoid — couples business code to Spring Security and the active strategy
var auth = SecurityContextHolder.getContext().getAuthentication();
```

### Return the standard envelopes

- Collections → `PagedResponse.from(page, Dto::from)`.
- Errors → throw `QavoException` subtypes; let the global handler format them.
- Never hand-roll an error JSON shape; it breaks the cross-application contract.

### Immutable DTOs, constructor injection

Use `record` DTOs. Inject dependencies through the constructor (the platform uses constructor
injection exclusively). Field injection is an anti-pattern here.

## 2. Extension guidelines

| You want to… | Use | Don't |
|---|---|---|
| Add an error category | Implement `ProblemType` + a `QavoException` subclass | Catch and reformat in each controller |
| Open an endpoint publicly | `PublicPathContributor` bean or `qavo.security.public-paths` | Redefine the whole `SecurityFilterChain` |
| Add security config | `HttpSecurityCustomizer` bean | Replace `qavoSecurityFilterChain` |
| Add a capability | A new plugin module | Grow the core |
| Add DB tables | A migration in your module's reserved location | Use `ddl-auto: update` |
| Call an upstream HTTP service | `QavoHttpClient` from `qavo-resilience` (declarative retry/circuit-breaker + traceId) | Raw `RestTemplate`/`RestClient` without timeouts |
| Track who created/modified a row | Extend `AuditableEntity` from `qavo-auditing` | Hand-roll `@PrePersist`/`@PreUpdate` per entity |

Override any platform bean by declaring your own of the same type — every platform bean is
`@ConditionalOnMissingBean`.

## 3. Plugin development guide

A plugin is a self-contained Maven module that the application opts into. To create one:

1. **New module** `qavo-<capability>` depending on `qavo-core` (and `qavo-security` if it touches
   auth). Add it to `qavo-bom`.

2. **Auto-configuration**, conditional and override-friendly:

   ```java
   @AutoConfiguration
   @ConditionalOnWebApplication(type = SERVLET)
   @ConditionalOnProperty(prefix = "qavo.<capability>", name = "enabled",
                          havingValue = "true", matchIfMissing = true)
   @EnableConfigurationProperties(MyPluginProperties.class)
   public class MyPluginAutoConfiguration {

       @Bean MyController myController(MyService s) { return new MyController(s); }

       @Bean QavoPlugin myPlugin() {
           return new PluginDescriptor("<capability>", "My Capability", "0.0.1-SNAPSHOT", "...");
       }

       @Bean PublicPathContributor myPublicPaths() {
           return () -> List.of(ApiConventions.BASE_PATH + "/<capability>/public/**");
       }

       @Bean MigrationLocation myMigrations() {
           return MigrationLocation.of("classpath:db/qavo/<capability>", "qavo-<capability>");
       }
   }
   ```

3. **Register** it in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

4. **Migrations** under `classpath:db/qavo/<capability>` (outside the application's `db/migration`
   tree), version-banded in `V0001`–`V0099` on a sub-range you do not share with another plugin.

5. **Routes** under a reserved namespace (e.g. `/api/v1/<capability>/**`).

6. **Tests** with `qavo-test-support`.

**Golden rule:** what is not imported does not exist in the application. A plugin must add nothing —
no beans, endpoints, tables, or attack surface — unless the application imported it.

## 4. Security best practices

- **Secure by default; loosen only deliberately.** Tightening headers is encouraged; loosening
  requires an explicit, reviewed override.
- **Never store plaintext credentials.** Use the provided `PasswordEncoder` (BCrypt). Never log
  passwords or tokens.
- **Authorize at the method level** with `@PreAuthorize` over the uniform permission model
  (`hasAuthority('resource:action')`), in addition to URL rules.
- **Keep secrets out of the repository.** Supply them via environment variables; encryption-at-rest
  is delegated to the infrastructure layer (architecture §5.5).
- **No raw SQL.** All data access goes through JPA/repositories — this is the platform's input
  sanitization stance, enforced structurally.
- **CORS is restrictive by default.** Declare explicit origins per deployment; avoid wildcards with
  credentials.

## 5. Testing best practices

- **Unit tests** for domain and application logic — fast, no Spring context.
- **Slice tests** (`@WebMvcTest`) for controllers and the error contract; assert with
  `ProblemDetailAssertions`.
- **Integration tests** against a real database via `AbstractPostgresIntegrationTest`
  (Testcontainers) — never mock the database for migration/JPA verification.
- **Plugin tests**: assert presence via `PluginRegistry`, and that removing the plugin removes its
  endpoints (contract test in the reference app).
- Keep the reference app green: it is the platform's end-to-end integration check.

## 6. Migration & versioning practices

- **Version banding:** platform reserves `V0001`–`V0099`; applications start at `V0100`. Each plugin
  uses a disjoint sub-range so versions stay globally unique in the shared Flyway history.
- **Forward-only migrations.** Never edit an applied migration; add a new one.
- **One concern per migration.** Keep them small and reviewable.
- **Semantic Versioning for artifacts** (architecture §3.2): PATCH = safe fixes, MINOR = additive,
  MAJOR = breaking (with a migration guide). The error contract and any token contracts are part of
  the compatibility surface.

## 7. Anti-patterns to avoid

| Anti-pattern | Why it hurts | Instead |
|---|---|---|
| "God core" — every feature in the core behind a flag | Unbounded core, attack surface for unused features, coupled releases (architecture §6.1) | Ship capabilities as plugins |
| Field injection (`@Autowired` fields) | Hidden dependencies, untestable | Constructor injection |
| Reaching into `SecurityContextHolder` | Couples business code to the auth strategy | `SecurityContextAccessor` |
| Hand-rolled error JSON | Breaks the cross-app contract | Throw `QavoException`; let the handler format |
| `spring.jpa.hibernate.ddl-auto: update` in prod | Schema drift, no audit trail | Flyway migrations |
| Exposing Spring Data `Page` directly | Unstable JSON shape across versions | `PagedResponse` |
| Catch-all `try/catch` in controllers | Defeats centralized handling | Let exceptions propagate |
| Disabling secure headers globally for convenience | Removes a baseline guarantee silently | Narrow, reviewed overrides |
