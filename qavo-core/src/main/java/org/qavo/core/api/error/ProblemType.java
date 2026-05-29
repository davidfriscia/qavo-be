/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.api.error;

import org.springframework.http.HttpStatus;

/**
 * Stable, machine-readable classification of an error condition.
 *
 * <p>Each {@code ProblemType} maps to a {@code type} URI and a default {@code title} in the
 * RFC 9457 Problem Details response (see architecture &sect;5.2). The platform ships
 * {@link CoreProblemType} for the common cases; applications and plugins implement this
 * interface to contribute their own domain-specific types without modifying the core.
 */
public interface ProblemType {

    /** Stable, kebab-case identifier appended to the configured error base URI (e.g. {@code validation}). */
    String code();

    /** Human-readable summary used as the Problem Details {@code title}. */
    String defaultTitle();

    /** HTTP status associated with this problem type. */
    HttpStatus status();
}
