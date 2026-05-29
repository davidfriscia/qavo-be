/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package com.example.referenceapp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.qavo.core.plugin.PluginRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: boots the full application context against H2, exercising Flyway migrations (platform
 * + plugins + application) and the entire auto-configuration chain. Also asserts that the imported
 * plugins are discovered by the platform's plugin registry — the architecture's plugin model
 * (&sect;6) working end to end.
 */
@SpringBootTest
class ReferenceApplicationTests {

    @Autowired
    private PluginRegistry pluginRegistry;

    @Test
    void contextLoadsAndPluginsAreRegistered() {
        assertThat(pluginRegistry.isPresent("auth-login")).isTrue();
        assertThat(pluginRegistry.isPresent("auth-registration")).isTrue();
    }
}
