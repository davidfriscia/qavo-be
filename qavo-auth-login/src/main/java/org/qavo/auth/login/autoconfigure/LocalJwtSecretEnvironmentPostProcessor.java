/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.autoconfigure;

import java.util.Base64;

import org.qavo.auth.login.jwt.JjwtTokenService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

/**
 * Validates the local JWT signing material at startup so misconfiguration fails fast with a
 * clear message — rather than surfacing as a cryptic runtime error the first time someone tries
 * to log in. Runs as an {@link EnvironmentPostProcessor} so the check executes before any
 * auto-configuration class is instantiated.
 *
 * <p>The check is silent unless the local authentication strategy is active AND the login plugin
 * is on the classpath (i.e. this very class loads). When both hold, the secret property must be
 * present and decode to at least 32 bytes (HMAC-SHA256 minimum, RFC 7518 §3.2).
 */
public class LocalJwtSecretEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String STRATEGY_PROPERTY = "qavo.security.strategy";
    private static final String SECRET_PROPERTY = "qavo.security.local.jwt.secret";
    private static final int MIN_KEY_BYTES = 32;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // The plugin is registered as an auto-configuration; if the issuance code path is not on
        // the classpath there is nothing to validate. This guard keeps the check OIDC-only-app safe.
        if (!ClassUtils.isPresent("org.qavo.auth.login.jwt.JjwtTokenService", getClass().getClassLoader())) {
            return;
        }
        if (Boolean.FALSE.equals(environment.getProperty("qavo.auth.login.enabled", Boolean.class, true))) {
            return;
        }
        String strategy = environment.getProperty(STRATEGY_PROPERTY, "local").trim().toLowerCase();
        if (!strategy.equals("local") && !strategy.equals("hybrid")) {
            return;
        }

        String secret = environment.getProperty(SECRET_PROPERTY);
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Property '" + SECRET_PROPERTY + "' must be set to a Base64-encoded HMAC-SHA256 key"
                            + " (at least " + MIN_KEY_BYTES + " bytes) when the local authentication"
                            + " strategy is active and the qavo-auth-login plugin is on the classpath."
                            + " Disable the plugin with qavo.auth.login.enabled=false to skip this check.");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Property '" + SECRET_PROPERTY
                    + "' is not valid Base64", ex);
        }
        if (decoded.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("Property '" + SECRET_PROPERTY
                    + "' must decode to at least " + MIN_KEY_BYTES + " bytes (HMAC-SHA256). Got "
                    + decoded.length + " bytes.");
        }
        // Touch the class so the JIT keeps the validation referenced and a future refactor that
        // accidentally removes the issuance code breaks this guard too.
        Class<?> referenced = JjwtTokenService.class;
        if (referenced.getName().isEmpty()) {
            throw new IllegalStateException("Unreachable");
        }
    }

    @Override
    public int getOrder() {
        // Run after the standard Spring Boot property-source contributors so user-supplied
        // application.yml / environment variables are visible, but before user-defined ones with
        // higher precedence; the precise position doesn't matter as long as we read after config.
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}
