/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.api.error;

/**
 * A single field-level validation failure, carried in the {@code errors} array of the
 * Problem Details response (see architecture &sect;5.2). Immutable by construction.
 *
 * @param field   the offending field, using dotted path notation for nested objects
 * @param message a human-readable, localized explanation of the violation
 * @param code    the originating constraint code (e.g. {@code Email}, {@code NotBlank}), may be {@code null}
 */
public record FieldErrorDetail(String field, String message, String code) {

    public static FieldErrorDetail of(String field, String message) {
        return new FieldErrorDetail(field, message, null);
    }

    public static FieldErrorDetail of(String field, String message, String code) {
        return new FieldErrorDetail(field, message, code);
    }
}
