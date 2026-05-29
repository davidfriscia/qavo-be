/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.security;

import java.util.Map;
import java.util.Set;

/**
 * Strategy-independent view of the authenticated user.
 *
 * <p>This is the uniform security-context abstraction described in architecture &sect;5.5: the
 * same shape is presented whether the active authentication strategy is local DB, OIDC, or a
 * hybrid of both. Business code depends only on this interface, so swapping the strategy never
 * touches application logic.
 */
public interface AuthenticatedPrincipal {

    /** Stable unique identifier of the principal (subject), as a string. */
    String id();

    /** Human-readable login name (username or email), may equal {@link #id()}. */
    String username();

    /** Granted roles, without the {@code ROLE_} prefix (e.g. {@code ADMIN}). */
    Set<String> roles();

    /** Fine-grained permissions/authorities the principal holds (e.g. {@code user:read}). */
    Set<String> permissions();

    /** Additional, strategy-specific claims or attributes. Never {@code null}. */
    Map<String, Object> attributes();
}
