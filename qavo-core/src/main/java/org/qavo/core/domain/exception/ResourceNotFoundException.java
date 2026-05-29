/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.domain.exception;

import org.qavo.core.api.error.CoreProblemType;

/**
 * Signals that a requested resource does not exist. Maps to HTTP 404.
 */
public class ResourceNotFoundException extends QavoException {

    public ResourceNotFoundException(String message) {
        super(CoreProblemType.RESOURCE_NOT_FOUND, message);
    }

    /** Convenience factory producing a consistent message such as {@code User with id '42' was not found}. */
    public static ResourceNotFoundException of(String resourceType, Object identifier) {
        return new ResourceNotFoundException(
                "%s with id '%s' was not found".formatted(resourceType, identifier));
    }
}
