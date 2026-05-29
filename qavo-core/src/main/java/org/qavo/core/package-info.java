/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */

/**
 * Qavo core: the small, stable foundation every Qavo-based application depends on.
 *
 * <p>The core defines <em>contracts</em>, not capabilities. It is organized along the platform's
 * layering convention (architecture &sect;4):
 * <ul>
 *   <li>{@code org.qavo.core.api} — presentation-layer contracts: API conventions, the RFC 9457
 *       error model, and the pagination envelope.</li>
 *   <li>{@code org.qavo.core.domain} — framework-independent domain contracts, notably the
 *       exception hierarchy.</li>
 *   <li>{@code org.qavo.core.security} — the strategy-independent security-context abstraction.</li>
 *   <li>{@code org.qavo.core.observability} — correlation-context contracts and MDC keys.</li>
 *   <li>{@code org.qavo.core.plugin} — the plugin SPI and registry.</li>
 *   <li>{@code org.qavo.core.feature} — the feature-flag abstraction.</li>
 *   <li>{@code org.qavo.core.config} — strongly-typed {@code qavo.*} configuration properties.</li>
 *   <li>{@code org.qavo.core.autoconfigure} — Spring Boot auto-configuration that wires the above.</li>
 * </ul>
 *
 * <p>Capabilities (login, registration, and so on) are delivered as separate, independently
 * versioned plugin modules — never added here. This keeps the core slow-moving and low-risk.
 */
package org.qavo.core;
