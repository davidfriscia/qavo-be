/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auditing;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for platform JPA auditing. Bound from {@code qavo.auditing.*}.
 */
@ConfigurationProperties(prefix = "qavo.auditing")
public class QavoAuditingProperties {

    /** Master switch — when {@code false} the auditing autoconfig short-circuits. */
    private boolean enabled = true;

    /**
     * Principal id recorded for writes that happen outside an authenticated request (background
     * jobs, scheduled tasks, data loaders). Recorded literally in {@code created_by} and
     * {@code last_modified_by} so downstream auditing queries can distinguish system-driven
     * changes from user-driven ones.
     */
    private String systemPrincipal = "system";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSystemPrincipal() {
        return systemPrincipal;
    }

    public void setSystemPrincipal(String systemPrincipal) {
        this.systemPrincipal = systemPrincipal;
    }
}
