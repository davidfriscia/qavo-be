/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Behavior of the registration plugin under {@code qavo.auth.registration.*} (architecture
 * &sect;6.1). Once the plugin is imported, these flags tune its runtime behavior — for example
 * whether self-service sign-up is currently open and whether email verification is required.
 */
@ConfigurationProperties(prefix = "qavo.auth.registration")
public class QavoRegistrationProperties {

    /** Whether the registration plugin is active. */
    private boolean enabled = true;

    /** Whether self-service (public) registration is currently open. */
    private boolean selfService = true;

    /** Whether newly registered accounts must verify their email before becoming active. */
    private boolean requireEmailVerification = false;

    /** Role granted to newly registered users. */
    private String defaultRole = "USER";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSelfService() {
        return selfService;
    }

    public void setSelfService(boolean selfService) {
        this.selfService = selfService;
    }

    public boolean isRequireEmailVerification() {
        return requireEmailVerification;
    }

    public void setRequireEmailVerification(boolean requireEmailVerification) {
        this.requireEmailVerification = requireEmailVerification;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }
}
