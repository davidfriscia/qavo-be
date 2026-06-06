/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.config;

import java.time.Duration;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;

/**
 * Configuration block bound to {@code qavo.auth.registration.cap.*} (architecture §6.1).
 * Activates the rolling-window cap that protects the deployment from registering more users
 * than the operator has provisioned capacity for. The feature is opt-in: when
 * {@code enabled=false} (the default), the platform registers a no-op service and the
 * registration controller behaves exactly as it did before the feature was introduced.
 *
 * <p>Fail-fast validation runs at startup so an obviously broken configuration
 * ({@code enabled=true} with an unset cap or window) surfaces immediately rather than at the
 * first registration attempt. See ADR 0012.
 */
@ConfigurationProperties(prefix = "qavo.auth.registration.cap")
public class QavoRegistrationCapProperties {

    /** Master switch. When {@code false}, no cap logic is applied. */
    private boolean enabled = false;

    /**
     * Maximum number of successful registrations allowed within the configured {@link #window}.
     * Must be ≥ 1 when {@link #enabled} is {@code true}.
     */
    private Integer maxRegistrations;

    /**
     * Rolling time window the counter slides over. Must be strictly positive when
     * {@link #enabled} is {@code true}. ISO-8601 duration syntax (e.g. {@code PT1H},
     * {@code PT24H}, {@code P7D}).
     */
    private Duration window;

    /**
     * When {@code true} (the default), every registration is counted regardless of whether
     * the user has verified their email; when {@code false}, only users with
     * {@code email_verified=true} contribute to the count, so unconfirmed "ghost"
     * registrations no longer consume capacity.
     */
    private boolean includeUnverified = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getMaxRegistrations() {
        return maxRegistrations;
    }

    public void setMaxRegistrations(Integer maxRegistrations) {
        this.maxRegistrations = maxRegistrations;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }

    public boolean isIncludeUnverified() {
        return includeUnverified;
    }

    public void setIncludeUnverified(boolean includeUnverified) {
        this.includeUnverified = includeUnverified;
    }

    @PostConstruct
    void validate() {
        if (!enabled) {
            return;
        }
        if (maxRegistrations == null || maxRegistrations < 1) {
            throw new BeanCreationException(
                    "qavo.auth.registration.cap.max-registrations must be set to a value >= 1 "
                            + "when qavo.auth.registration.cap.enabled=true");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new BeanCreationException(
                    "qavo.auth.registration.cap.window must be set to a strictly positive "
                            + "duration when qavo.auth.registration.cap.enabled=true");
        }
    }
}
