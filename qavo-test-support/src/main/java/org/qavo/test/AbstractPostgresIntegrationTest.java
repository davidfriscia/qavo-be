/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that need a real PostgreSQL database (architecture &sect;8).
 *
 * <p>Extending this class starts a single, shared PostgreSQL container for the test class and wires
 * its JDBC coordinates into the Spring {@code Environment}, so tests exercise Flyway migrations and
 * JPA against a production-like engine rather than an in-memory substitute. The container is reused
 * across test methods for speed and torn down by the Testcontainers lifecycle.
 *
 * <pre>{@code
 * @SpringBootTest
 * class UserRepositoryIT extends AbstractPostgresIntegrationTest {
 *     // @Autowired beans run against the live container
 * }
 * }</pre>
 */
@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("qavo")
                    .withUsername("qavo")
                    .withPassword("qavo");

    static {
        // Manual start with reuse keeps a single container across all subclasses in a JVM run.
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
