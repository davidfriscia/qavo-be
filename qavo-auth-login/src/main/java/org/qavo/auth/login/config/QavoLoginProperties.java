/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Behavior of the login plugin under {@code qavo.auth.login.*}. Illustrates the platform's
 * "modularity for distribution, configuration for behavior" split (architecture &sect;6.1): the
 * plugin exists only if imported, and once imported its behavior is tuned here.
 */
@ConfigurationProperties(prefix = "qavo.auth.login")
public class QavoLoginProperties {

    /** Whether the login endpoint is active. */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
