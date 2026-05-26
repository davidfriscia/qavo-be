/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.web;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Extension point for contributing additional configuration to the platform's
 * {@link org.springframework.security.web.SecurityFilterChain} without replacing it.
 *
 * <p>The base filter chain applies every {@code HttpSecurityCustomizer} bean it finds, ordered by
 * the standard Spring {@code @Order}. This is how the OIDC strategy adds resource-server
 * configuration, and how plugins (e.g. the login plugin) register their own authorization rules,
 * while the secure-by-default baseline stays in one place (see architecture &sect;5.5).
 */
@FunctionalInterface
public interface HttpSecurityCustomizer {

    void customize(HttpSecurity http) throws Exception;
}
