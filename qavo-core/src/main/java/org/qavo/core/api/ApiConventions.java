/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.api;

/**
 * Centralized REST conventions shared by every Qavo-based application.
 *
 * <p>These constants encode the platform's path-based API versioning strategy (see
 * architecture &sect;5.1): the version segment is always part of the URL, never a header or
 * query parameter. Applications and plugins build their routes on top of {@link #BASE_PATH}
 * so that the contract is visible at a glance in every request and "API sprawl" is avoided.
 */
public final class ApiConventions {

    /** Common prefix for all platform and application endpoints. */
    public static final String API_PREFIX = "/api";

    /** Current major API version segment. A breaking change bumps both this and the artifact MAJOR. */
    public static final String CURRENT_VERSION = "v1";

    /** Fully-qualified base path inherited by application controllers (e.g. {@code /api/v1}). */
    public static final String BASE_PATH = API_PREFIX + "/" + CURRENT_VERSION;

    /** Reserved namespace under which authentication plugins mount their routes. */
    public static final String AUTH_NAMESPACE = BASE_PATH + "/auth";

    /** Standard query parameter carrying the zero-based page index. */
    public static final String PAGE_PARAM = "page";

    /** Standard query parameter carrying the page size. */
    public static final String SIZE_PARAM = "size";

    /** Standard query parameter carrying sort directives ({@code field,asc|desc}). */
    public static final String SORT_PARAM = "sort";

    private ApiConventions() {
        throw new AssertionError("Constants holder must not be instantiated");
    }
}
