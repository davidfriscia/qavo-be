/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.it;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test-only Spring Boot application that bootstraps just the platform autoconfigurations needed
 * to exercise the registration plugin against a real database. Lives under {@code src/test} so it
 * never ships in the plugin artifact and never affects consuming applications.
 */
@SpringBootApplication
public class RegistrationTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(RegistrationTestApplication.class, args);
    }
}
